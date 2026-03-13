package com.gostock.service;

import com.gostock.dto.*;
import com.gostock.entity.*;
import com.gostock.entity.enums.*;
import com.gostock.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class TransactionService {

    private static final BigDecimal TAX_RATE = new BigDecimal("0.001"); // 0.1% thuế bán

    private final TransactionRepository transactionRepo;
    private final TickerRepository tickerRepo;
    private final AccountRepository accountRepo;
    private final PositionRepository positionRepo;
    private final PriceHistoryRepository priceHistoryRepo;

    // ── Tạo giao dịch ────────────────────────────────────────────────────

    public TransactionResponse create(TransactionRequest req) {
        Account account = accountRepo.findById(req.getAccountId())
                .orElseThrow(() -> new EntityNotFoundException("Account not found: " + req.getAccountId()));
        Ticker ticker = tickerRepo.findBySymbolIgnoreCase(req.getTickerSymbol())
                .orElseThrow(() -> new EntityNotFoundException("Ticker not found: " + req.getTickerSymbol()));

        Transaction tx = buildTransaction(req, account, ticker);
        tx = transactionRepo.save(tx);
        updatePosition(account, ticker, tx);
        return toResponse(tx);
    }

    // ── Sửa giao dịch ────────────────────────────────────────────────────

    public TransactionResponse update(Long id, TransactionRequest req) {
        Transaction tx = transactionRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found: " + id));
        Account account = accountRepo.findById(req.getAccountId())
                .orElseThrow(() -> new EntityNotFoundException("Account not found: " + req.getAccountId()));
        Ticker ticker = tickerRepo.findBySymbolIgnoreCase(req.getTickerSymbol())
                .orElseThrow(() -> new EntityNotFoundException("Ticker not found: " + req.getTickerSymbol()));

        updateTransactionFields(tx, req, account, ticker);
        return toResponse(transactionRepo.save(tx));
    }

    // ── Xem danh sách (phân trang, lọc, sắp xếp) ────────────────────────

    @Transactional(readOnly = true)
    public Page<TransactionResponse> search(String tickerSymbol, TradeType trade,
                                            LocalDate fromDate, LocalDate toDate,
                                            Long accountId, Pageable pageable) {
        List<Transaction> all = transactionRepo.search(tickerSymbol, trade, fromDate, toDate, accountId);

        // manual paging (vì JPQL + Pageable + countQuery phức tạp)
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), all.size());
        List<TransactionResponse> page = all.subList(start, end).stream().map(this::toResponse).toList();
        return new PageImpl<>(page, pageable, all.size());
    }

    // ── Import từ Excel ───────────────────────────────────────────────────

    public List<TransactionResponse> importFromExcel(MultipartFile file, Long accountId) throws IOException {
        return importByType(file, accountId, null);
    }

    public List<TransactionResponse> importStockTransactionHistoryExcel(MultipartFile file, Long accountId) throws IOException {
        return importByType(file, accountId, ImportSheetType.STOCK_TRANSACTION_HISTORY);
    }

    public List<TransactionResponse> importFundStatementExcel(MultipartFile file, Long accountId) throws IOException {
        return importByType(file, accountId, ImportSheetType.FUND_STATEMENT);
    }

    public List<TransactionResponse> importStockStatementExcel(MultipartFile file, Long accountId) throws IOException {
        return importByType(file, accountId, ImportSheetType.STOCK_STATEMENT);
    }

    private List<TransactionResponse> importByType(
            MultipartFile file,
            Long accountId,
            ImportSheetType expectedType) throws IOException {
        Account account = accountRepo.findById(accountId)
                .orElseThrow(() -> new EntityNotFoundException("Account not found: " + accountId));

        try (Workbook wb = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            ImportSheetType detectedType = detectSheetType(sheet);

            if (expectedType != null && detectedType != expectedType) {
                throw new IllegalArgumentException("File không đúng mẫu " + expectedType.getDisplayName() + ".");
            }

            return switch (detectedType) {
                case STOCK_TRANSACTION_HISTORY -> importStockTransactionHistory(sheet, account);
                case FUND_STATEMENT -> importFundStatement(sheet, account);
                case STOCK_STATEMENT -> importStockStatement(sheet, account);
                case CASH_STATEMENT -> throw new IllegalArgumentException(
                        "File này là SAO KÊ TIỀN. Vui lòng import tại màn Quản lý Tiền mặt.");
                case UNKNOWN -> throw new IllegalArgumentException(
                        "Không nhận diện được mẫu file. Hỗ trợ: Stock Transaction History, Fund Statement, Stock Statement.");
            };
        }
    }

    private List<TransactionResponse> importStockTransactionHistory(Sheet sheet, Account account) {
        int headerRow = findHeaderRow(sheet, "ticker", "tradingdate", "trade", "matchedvolume");
        if (headerRow < 0) {
            throw new IllegalArgumentException("Không tìm thấy header của STOCK TRANSACTION HISTORY.");
        }

        DataFormatter formatter = new DataFormatter();
        List<TransactionResponse> results = new ArrayList<>();

        for (int i = headerRow + 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            String symbol = normalizeSymbol(getCellString(row, 0, formatter));
            if (symbol == null) continue;

            LocalDate tradingDate = getCellDate(row, 1, formatter);
            if (tradingDate == null) continue;

            TradeType trade = parseTrade(getCellString(row, 2, formatter), null);
            if (trade == null) continue;

            long volume = parseLongCell(row, 5, formatter, 0L);
            if (volume <= 0) {
                volume = parseLongCell(row, 3, formatter, 0L);
            }
            if (volume <= 0) continue;

            BigDecimal orderPrice = parseDecimalCell(row, 4, formatter, BigDecimal.ZERO);
            BigDecimal matchedPrice = parseDecimalCell(row, 6, formatter, BigDecimal.ZERO);
            if (matchedPrice.compareTo(BigDecimal.ZERO) <= 0) {
                matchedPrice = orderPrice;
            }
            if (orderPrice.compareTo(BigDecimal.ZERO) <= 0) {
                orderPrice = matchedPrice;
            }
            if (matchedPrice.compareTo(BigDecimal.ZERO) <= 0) continue;

            String orderNo = getCellString(row, 15, formatter);
            if (orderNo == null) {
                orderNo = syntheticOrderNo("STX", tradingDate, symbol, trade, volume, i);
            }
            if (transactionRepo.existsByOrderNoAndAccount_Id(orderNo, account.getId())) continue;

            Ticker ticker = findOrCreateTicker(symbol, InstrumentType.STOCK);

            TransactionRequest req = new TransactionRequest();
            req.setAccountId(account.getId());
            req.setTickerSymbol(ticker.getSymbol());
            req.setTradingDate(tradingDate);
            req.setTrade(trade);
            req.setVolume(volume);
            req.setMatchedVolume(volume);
            req.setOrderPrice(orderPrice);
            req.setMatchedValue(matchedPrice);
            req.setFee(parseDecimalCell(row, 8, formatter, null));
            req.setTax(parseDecimalCell(row, 9, formatter, null));
            req.setChannel(getCellString(row, 12, formatter));
            req.setOrderType(parseOrderType(getCellString(row, 14, formatter)));
            req.setOrderNo(orderNo);
            req.setStockExchange(ticker.getExchange());
            req.setNote("Imported from STOCK TRANSACTION HISTORY");

            tryImportCreate(results, req);
        }

        return results;
    }

    private List<TransactionResponse> importFundStatement(Sheet sheet, Account account) {
        int headerRow = findHeaderRow(sheet, "fund", "tradingdate", "trade", "costunit", "matchedvolume");
        if (headerRow < 0) {
            throw new IllegalArgumentException("Không tìm thấy header của FUND STATEMENT.");
        }

        DataFormatter formatter = new DataFormatter();
        List<TransactionResponse> results = new ArrayList<>();

        for (int i = headerRow + 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            String symbol = normalizeSymbol(getCellString(row, 0, formatter));
            if (symbol == null) continue;

            TradeType trade = parseTrade(getCellString(row, 1, formatter), null);
            if (trade == null) continue;

            LocalDate tradingDate = getCellDate(row, 2, formatter);
            if (tradingDate == null) continue;

            BigDecimal rawVolume = parseDecimalCell(row, 4, formatter, BigDecimal.ZERO).abs();
            if (rawVolume.compareTo(BigDecimal.ZERO) <= 0) continue;

            long volume = rawVolume.setScale(0, RoundingMode.HALF_UP).longValue();
            if (volume <= 0) volume = 1L;

            BigDecimal unitPrice = parseDecimalCell(row, 3, formatter, BigDecimal.ZERO);
            if (unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
                BigDecimal totalAmount = parseDecimalCell(row, 6, formatter, BigDecimal.ZERO);
                if (totalAmount.compareTo(BigDecimal.ZERO) > 0) {
                    unitPrice = totalAmount.divide(rawVolume, 2, RoundingMode.HALF_UP);
                }
            }
            if (unitPrice.compareTo(BigDecimal.ZERO) <= 0) continue;

            String orderNo = syntheticOrderNo("FND", tradingDate, symbol, trade, volume, i);
            if (transactionRepo.existsByOrderNoAndAccount_Id(orderNo, account.getId())) continue;

            Ticker ticker = findOrCreateTicker(symbol, InstrumentType.FUND_CERTIFICATE);

            TransactionRequest req = new TransactionRequest();
            req.setAccountId(account.getId());
            req.setTickerSymbol(ticker.getSymbol());
            req.setTradingDate(tradingDate);
            req.setTrade(trade);
            req.setVolume(volume);
            req.setMatchedVolume(volume);
            req.setOrderPrice(unitPrice);
            req.setMatchedValue(unitPrice);
            req.setFee(BigDecimal.ZERO);
            req.setTax(BigDecimal.ZERO);
            req.setOrderType(OrderType.NORMAL);
            req.setOrderNo(orderNo);
            req.setChannel("FUND");
            req.setNote("Imported from FUND STATEMENT: " + Optional.ofNullable(getCellString(row, 7, formatter)).orElse(""));

            tryImportCreate(results, req);
        }

        return results;
    }

    private List<TransactionResponse> importStockStatement(Sheet sheet, Account account) {
        int headerRow = findHeaderRow(sheet, "ticker", "tradingdate", "actions", "volume");
        if (headerRow < 0) {
            throw new IllegalArgumentException("Không tìm thấy header của STOCK STATEMENT.");
        }

        DataFormatter formatter = new DataFormatter();
        List<TransactionResponse> results = new ArrayList<>();

        for (int i = headerRow + 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            String symbol = normalizeSymbol(getCellString(row, 0, formatter));
            if (symbol == null) continue;

            LocalDate tradingDate = getCellDate(row, 1, formatter);
            if (tradingDate == null) continue;

            BigDecimal signedVolume = parseDecimalCell(row, 3, formatter, BigDecimal.ZERO);
            if (signedVolume.compareTo(BigDecimal.ZERO) == 0) continue;

            TradeType trade = parseTrade(getCellString(row, 2, formatter), signedVolume);
            if (trade == null) continue;

            long volume = signedVolume.abs().setScale(0, RoundingMode.HALF_UP).longValue();
            if (volume <= 0) continue;

            String orderNo = syntheticOrderNo("STM", tradingDate, symbol, trade, volume, i);
            if (transactionRepo.existsByOrderNoAndAccount_Id(orderNo, account.getId())) continue;

            Ticker ticker = findOrCreateTicker(symbol, InstrumentType.STOCK);
            BigDecimal approxPrice = resolveApproxPriceForStatement(account.getId(), ticker, trade);

            TransactionRequest req = new TransactionRequest();
            req.setAccountId(account.getId());
            req.setTickerSymbol(ticker.getSymbol());
            req.setTradingDate(tradingDate);
            req.setTrade(trade);
            req.setVolume(volume);
            req.setMatchedVolume(volume);
            req.setOrderPrice(approxPrice);
            req.setMatchedValue(approxPrice);
            req.setFee(BigDecimal.ZERO);
            req.setTax(BigDecimal.ZERO);
            req.setOrderType(OrderType.NORMAL);
            req.setOrderNo(orderNo);
            req.setChannel("STATEMENT");
            req.setStockExchange(ticker.getExchange());
            req.setNote("Imported from STOCK STATEMENT: " + Optional.ofNullable(getCellString(row, 4, formatter)).orElse(""));

            tryImportCreate(results, req);
        }

        return results;
    }

    private void tryImportCreate(List<TransactionResponse> results, TransactionRequest req) {
        try {
            results.add(create(req));
        } catch (DataIntegrityViolationException ignored) {
            // Skip duplicate/constraint-violating rows to keep import running.
        }
    }

    private ImportSheetType detectSheetType(Sheet sheet) {
        StringBuilder sb = new StringBuilder();
        DataFormatter formatter = new DataFormatter();
        int last = Math.min(sheet.getLastRowNum(), 40);

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
        if (content.contains("cashstatement") || content.contains("saoketien")) {
            return ImportSheetType.CASH_STATEMENT;
        }
        if (content.contains("stocktransactionhistory") || content.contains("lichsugiaodichcophieu")) {
            return ImportSheetType.STOCK_TRANSACTION_HISTORY;
        }
        if (content.contains("fundstatement") || content.contains("lichsugiaodichquy")) {
            return ImportSheetType.FUND_STATEMENT;
        }
        if (content.contains("stockstatement") || content.contains("saokecophieu")) {
            return ImportSheetType.STOCK_STATEMENT;
        }
        return ImportSheetType.UNKNOWN;
    }

    private int findHeaderRow(Sheet sheet, String... headerTokens) {
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
            int matched = 0;
            for (String token : headerTokens) {
                if (normalizedLine.contains(normalizeToken(token))) matched++;
            }
            if (matched >= Math.max(3, headerTokens.length - 1)) {
                return i;
            }
        }
        return -1;
    }

    private Ticker findOrCreateTicker(String symbol, InstrumentType type) {
        String normalized = normalizeSymbol(symbol);
        if (normalized == null) {
            throw new IllegalArgumentException("Ticker symbol không hợp lệ.");
        }

        return tickerRepo.findBySymbolIgnoreCase(normalized)
                .orElseGet(() -> tickerRepo.save(Ticker.builder()
                        .symbol(normalized)
                        .name(normalized)
                        .type(type)
                        .active(true)
                        .build()));
    }

    private BigDecimal resolveApproxPriceForStatement(Long accountId, Ticker ticker, TradeType trade) {
        Optional<Position> positionOpt = positionRepo.findByAccount_IdAndTicker_Id(accountId, ticker.getId());
        if (trade == TradeType.SELL && positionOpt.isPresent()) {
            Position pos = positionOpt.get();
            if (pos.getAvgCost() != null && pos.getAvgCost().compareTo(BigDecimal.ZERO) > 0) {
                return pos.getAvgCost().setScale(2, RoundingMode.HALF_UP);
            }
        }

        if (positionOpt.isPresent()) {
            Position pos = positionOpt.get();
            if (pos.getCurrentPrice() != null && pos.getCurrentPrice().compareTo(BigDecimal.ZERO) > 0) {
                return pos.getCurrentPrice().setScale(2, RoundingMode.HALF_UP);
            }
            if (pos.getAvgCost() != null && pos.getAvgCost().compareTo(BigDecimal.ZERO) > 0) {
                return pos.getAvgCost().setScale(2, RoundingMode.HALF_UP);
            }
        }

        return priceHistoryRepo.findLatestByTickerId(ticker.getId())
                .map(PriceHistory::getClosePrice)
                .filter(price -> price.compareTo(BigDecimal.ZERO) > 0)
                .orElse(BigDecimal.ONE)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private TradeType parseTrade(String rawTrade, BigDecimal signedVolume) {
        String token = normalizeToken(rawTrade);
        if (token != null) {
            if (token.contains("buy") || token.contains("mua") || token.contains("nhan")) return TradeType.BUY;
            if (token.contains("sell") || token.contains("ban") || token.contains("rut") || token.contains("tra")) return TradeType.SELL;
        }

        if (signedVolume != null) {
            if (signedVolume.compareTo(BigDecimal.ZERO) > 0) return TradeType.BUY;
            if (signedVolume.compareTo(BigDecimal.ZERO) < 0) return TradeType.SELL;
        }
        return null;
    }

    private OrderType parseOrderType(String rawOrderType) {
        String token = normalizeToken(rawOrderType);
        if (token == null) return OrderType.NORMAL;
        if (token.contains("phaisinh") || token.contains("derivative")) return OrderType.DERIVATIVE;
        return OrderType.NORMAL;
    }

    private String syntheticOrderNo(String prefix, LocalDate date, String symbol, TradeType trade, long volume, int rowIndex) {
        return "%s-%s-%s-%s-%d-%d".formatted(prefix, date, symbol, trade.name(), volume, rowIndex + 1);
    }

    private String normalizeSymbol(String symbol) {
        if (symbol == null) return null;
        String trimmed = symbol.trim().toUpperCase(Locale.ROOT);
        if (trimmed.isEmpty()) return null;
        if (trimmed.contains("TICKER") || trimmed.contains("MÃ") || trimmed.contains("MA")) return null;
        return trimmed;
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
        String raw = formatter.formatCellValue(cell);
        if (raw == null) return null;
        String trimmed = raw.trim();
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
                // try next format
            }
        }
        return null;
    }

    private long parseLongCell(Row row, int col, DataFormatter formatter, long defaultValue) {
        BigDecimal number = parseDecimalCell(row, col, formatter, null);
        if (number == null) return defaultValue;
        return number.setScale(0, RoundingMode.HALF_UP).longValue();
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
                .replace("vnd", "");

        // Hỗ trợ format 28,400 hoặc 44,245.94
        normalized = normalized.replace(",", "");

        if (normalized.isBlank() || "-".equals(normalized)) return defaultValue;
        try {
            BigDecimal val = new BigDecimal(normalized);
            return negativeByParen ? val.negate() : val;
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private enum ImportSheetType {
        STOCK_TRANSACTION_HISTORY,
        FUND_STATEMENT,
        STOCK_STATEMENT,
        CASH_STATEMENT,
        UNKNOWN;

        public String getDisplayName() {
            return switch (this) {
                case STOCK_TRANSACTION_HISTORY -> "Stock Transaction History";
                case FUND_STATEMENT -> "Fund Statement";
                case STOCK_STATEMENT -> "Stock Statement";
                case CASH_STATEMENT -> "Cash Statement";
                case UNKNOWN -> "Unknown";
            };
        }
    }

    // ── Internal helpers ──────────────────────────────────────────────────

    private Transaction buildTransaction(TransactionRequest req, Account account, Ticker ticker) {
        BigDecimal matchedVol = BigDecimal.valueOf(req.getMatchedVolume() != null ? req.getMatchedVolume() : req.getVolume());
        BigDecimal matchedPrice = req.getMatchedValue() != null ? req.getMatchedValue() : req.getOrderPrice();
        BigDecimal tradeValue = matchedVol.multiply(matchedPrice);

        BigDecimal fee = req.getFee() != null ? req.getFee()
                : tradeValue.multiply(account.getBroker().getDefaultFeeRate()).setScale(0, RoundingMode.HALF_UP);

        BigDecimal tax = req.getTax() != null ? req.getTax()
                : (req.getTrade() == TradeType.SELL ? tradeValue.multiply(TAX_RATE).setScale(0, RoundingMode.HALF_UP) : BigDecimal.ZERO);

        BigDecimal cost = req.getTrade() == TradeType.BUY
                ? tradeValue.add(fee).add(tax)
                : BigDecimal.ZERO; // cost của lệnh bán sẽ được tính khi cập nhật Position

        return Transaction.builder()
                .account(account)
                .ticker(ticker)
                .orderNo(req.getOrderNo())
                .tradingDate(req.getTradingDate())
                .trade(req.getTrade())
                .volume(req.getVolume())
                .orderPrice(req.getOrderPrice())
                .matchedVolume(req.getMatchedVolume() != null ? req.getMatchedVolume() : req.getVolume())
                .matchedValue(matchedPrice)
                .stockExchange(req.getStockExchange())
                .orderType(req.getOrderType())
                .channel(req.getChannel())
                .fee(fee)
                .tax(tax)
                .cost(cost)
                .status(TransactionStatus.COMPLETED)
                .note(req.getNote())
                .build();
    }

    private void updateTransactionFields(Transaction tx, TransactionRequest req, Account account, Ticker ticker) {
        tx.setAccount(account);
        tx.setTicker(ticker);
        tx.setTradingDate(req.getTradingDate());
        tx.setTrade(req.getTrade());
        tx.setVolume(req.getVolume());
        tx.setOrderPrice(req.getOrderPrice());
        tx.setMatchedVolume(req.getMatchedVolume());
        tx.setMatchedValue(req.getMatchedValue());
        tx.setStockExchange(req.getStockExchange());
        tx.setOrderType(req.getOrderType());
        tx.setChannel(req.getChannel());
        tx.setNote(req.getNote());
    }

    /**
     * Cập nhật Position sau khi có giao dịch mới.
     * BUY:  holdingVolume += matchedVolume, tính lại avgCost theo FIFO bình quân gia quyền.
     * SELL: holdingVolume -= matchedVolume, tính realized PnL và ghi vào transaction.returnAmount.
     */
    private void updatePosition(Account account, Ticker ticker, Transaction tx) {
        Position pos = positionRepo.findByAccount_IdAndTicker_Id(account.getId(), ticker.getId())
                .orElseGet(() -> Position.builder().account(account).ticker(ticker)
                .holdingVolume(0L)
                .avgCost(BigDecimal.ZERO)
                .currentPrice(BigDecimal.ZERO)
                .unrealizedPnL(BigDecimal.ZERO)
                .build());

        long vol = tx.getMatchedVolume() != null ? tx.getMatchedVolume() : tx.getVolume();
        BigDecimal price = tx.getMatchedValue() != null ? tx.getMatchedValue() : tx.getOrderPrice();

        if (tx.getTrade() == TradeType.BUY) {
            BigDecimal oldTotal = pos.getAvgCost().multiply(BigDecimal.valueOf(pos.getHoldingVolume()));
            BigDecimal newTotal = price.multiply(BigDecimal.valueOf(vol)).add(tx.getFee());
            long newVol = pos.getHoldingVolume() + vol;
            pos.setAvgCost(newVol > 0
                    ? oldTotal.add(newTotal).divide(BigDecimal.valueOf(newVol), 4, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO);
            pos.setHoldingVolume(newVol);
        } else {
            // Realized PnL = (matchedPrice - avgCost) × vol - fee - tax
            BigDecimal realized = price.subtract(pos.getAvgCost())
                    .multiply(BigDecimal.valueOf(vol))
                    .subtract(tx.getFee())
                    .subtract(tx.getTax());
            tx.setReturnAmount(realized);
            tx.setCost(pos.getAvgCost().multiply(BigDecimal.valueOf(vol)));
            transactionRepo.save(tx);

            pos.setHoldingVolume(Math.max(0, pos.getHoldingVolume() - vol));
        }
        positionRepo.saveAndFlush(pos);
    }

    public TransactionResponse toResponse(Transaction tx) {
        return TransactionResponse.builder()
                .id(tx.getId())
                .orderNo(tx.getOrderNo())
                .tradingDate(tx.getTradingDate())
                .trade(tx.getTrade())
                .tickerSymbol(tx.getTicker().getSymbol())
                .tickerName(tx.getTicker().getName())
                .accountName(tx.getAccount().getName())
                .brokerCode(tx.getAccount().getBroker().getCode())
                .stockExchange(tx.getStockExchange())
                .orderType(tx.getOrderType())
                .channel(tx.getChannel())
                .volume(tx.getVolume())
                .orderPrice(tx.getOrderPrice())
                .matchedVolume(tx.getMatchedVolume())
                .matchedValue(tx.getMatchedValue())
                .fee(tx.getFee())
                .tax(tx.getTax())
                .cost(tx.getCost())
                .returnAmount(tx.getReturnAmount())
                .status(tx.getStatus())
                .note(tx.getNote())
                .createdAt(tx.getCreatedAt())
                .build();
    }

}
