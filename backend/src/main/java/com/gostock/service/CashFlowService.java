package com.gostock.service;

import com.gostock.dto.CashFlowRequest;
import com.gostock.entity.Account;
import com.gostock.entity.CashFlow;
import com.gostock.entity.enums.CashFlowType;
import com.gostock.repository.AccountRepository;
import com.gostock.repository.CashFlowRepository;
import com.gostock.service.contract.CashFlowServiceContract;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class CashFlowService implements CashFlowServiceContract {

    private final CashFlowRepository cashFlowRepo;
    private final AccountRepository accountRepo;

    @Override
    public CashFlow create(CashFlowRequest req) {
        Account account = accountRepo.findById(req.getAccountId())
                .orElseThrow(() -> new EntityNotFoundException("Account not found: " + req.getAccountId()));

        CashFlow cf = CashFlow.builder()
                .account(account)
                .type(req.getType())
                .amount(req.getAmount())
                .flowDate(req.getFlowDate())
                .note(req.getNote())
                .build();

        CashFlow saved = cashFlowRepo.save(cf);

        // Cập nhật số dư tài khoản
        if (req.getType() == CashFlowType.DEPOSIT) {
            account.setCashBalance(account.getCashBalance().add(req.getAmount()));
            account.setPurchasingPower(account.getPurchasingPower().add(req.getAmount()));
            account.setAvailableForWithdrawal(account.getAvailableForWithdrawal().add(req.getAmount()));
        } else {
            account.setCashBalance(account.getCashBalance().subtract(req.getAmount()));
            account.setPurchasingPower(account.getPurchasingPower().subtract(req.getAmount()));
            account.setAvailableForWithdrawal(account.getAvailableForWithdrawal().subtract(req.getAmount()));
        }
        accountRepo.save(account);

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CashFlow> listByAccount(Long accountId) {
        return cashFlowRepo.findByAccount_IdOrderByFlowDateDesc(accountId);
    }

    @Override
    public List<CashFlow> importFromExcel(MultipartFile file, Long accountId) throws IOException {
        accountRepo.findById(accountId)
                .orElseThrow(() -> new EntityNotFoundException("Account not found: " + accountId));

        try (Workbook wb = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            if (!isCashStatementSheet(sheet)) {
                throw new IllegalArgumentException("File không đúng mẫu SAO KÊ TIỀN / CASH STATEMENT.");
            }

            int headerRow = findHeaderRow(sheet);
            if (headerRow < 0) {
                throw new IllegalArgumentException("Không tìm thấy header dữ liệu cash statement.");
            }

            DataFormatter formatter = new DataFormatter();
            List<CashFlow> imported = new java.util.ArrayList<>();

            for (int i = headerRow + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                LocalDate flowDate = getCellDate(row, 0, formatter);
                if (flowDate == null) continue;

                BigDecimal signedAmount = parseDecimalCell(row, 2, formatter, null);
                if (signedAmount == null || signedAmount.compareTo(BigDecimal.ZERO) == 0) continue;

                CashFlowType type = signedAmount.compareTo(BigDecimal.ZERO) > 0
                        ? CashFlowType.DEPOSIT
                        : CashFlowType.WITHDRAW;
                BigDecimal amount = signedAmount.abs().setScale(2, RoundingMode.HALF_UP);

                String note = Optional.ofNullable(getCellString(row, 4, formatter)).orElse("").trim();
                if (note.length() > 255) {
                    note = note.substring(0, 255);
                }

                if (cashFlowRepo.existsByAccount_IdAndTypeAndAmountAndFlowDateAndNote(
                        accountId, type, amount, flowDate, note.isBlank() ? null : note)) {
                    continue;
                }

                CashFlowRequest req = new CashFlowRequest();
                req.setAccountId(accountId);
                req.setType(type);
                req.setAmount(amount);
                req.setFlowDate(flowDate);
                req.setNote(note.isBlank() ? null : note);

                imported.add(create(req));
            }

            return imported;
        }
    }

    private boolean isCashStatementSheet(Sheet sheet) {
        DataFormatter formatter = new DataFormatter();
        StringBuilder sb = new StringBuilder();
        int last = Math.min(sheet.getLastRowNum(), 30);

        for (int i = 0; i <= last; i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            short maxCell = row.getLastCellNum();
            if (maxCell < 0) continue;
            for (int c = 0; c < maxCell; c++) {
                String val = getCellString(row, c, formatter);
                if (val != null) sb.append(' ').append(normalizeToken(val));
            }
        }
        String content = sb.toString();
        return content.contains("cashstatement") || content.contains("saoketien");
    }

    private int findHeaderRow(Sheet sheet) {
        DataFormatter formatter = new DataFormatter();
        int last = Math.min(sheet.getLastRowNum(), 80);
        for (int i = 0; i <= last; i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            StringBuilder line = new StringBuilder();
            short maxCell = row.getLastCellNum();
            if (maxCell < 0) continue;
            for (int c = 0; c < maxCell; c++) {
                String val = getCellString(row, c, formatter);
                if (val != null) line.append(' ').append(normalizeToken(val));
            }

            String normalizedLine = line.toString();
            if (normalizedLine.contains("tradingdate")
                    && normalizedLine.contains("transactiontype")
                    && normalizedLine.contains("amount")) {
                return i;
            }
        }
        return -1;
    }

    private String normalizeToken(String value) {
        if (value == null) return null;
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");
        return normalized.isBlank() ? null : normalized;
    }

    private String getCellString(Row row, int col, DataFormatter formatter) {
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        String value = formatter.formatCellValue(cell);
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private LocalDate getCellDate(Row row, int col, DataFormatter formatter) {
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate();
        }

        String raw = getCellString(row, col, formatter);
        if (raw == null) return null;

        List<DateTimeFormatter> formats = List.of(
                DateTimeFormatter.ofPattern("d/M/uuuu"),
                DateTimeFormatter.ofPattern("dd/MM/uuuu"),
                DateTimeFormatter.ofPattern("uuuu-MM-dd")
        );
        for (DateTimeFormatter f : formats) {
            try {
                return LocalDate.parse(raw, f);
            } catch (DateTimeParseException ignored) {
                // continue
            }
        }
        return null;
    }

    private BigDecimal parseDecimalCell(Row row, int col, DataFormatter formatter, BigDecimal defaultValue) {
        String raw = getCellString(row, col, formatter);
        if (raw == null) return defaultValue;

        String normalized = raw.trim();
        boolean negativeByParen = normalized.startsWith("(") && normalized.endsWith(")");
        normalized = normalized.replace("(", "").replace(")", "")
                .replace("+", "")
                .replace(" ", "")
                .replace("VND", "")
                .replace("vnd", "")
                .replace(",", "");

        if (normalized.isBlank() || "-".equals(normalized)) return defaultValue;
        try {
            BigDecimal val = new BigDecimal(normalized);
            return negativeByParen ? val.negate() : val;
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }
}
