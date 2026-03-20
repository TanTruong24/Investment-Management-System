package com.gostock.controller;

import com.gostock.dto.*;
import com.gostock.entity.enums.TradeType;
import com.gostock.service.contract.TransactionServiceContract;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionServiceContract transactionService;

    /** Tạo giao dịch */
    @PostMapping
    public ResponseEntity<TransactionResponse> create(@Valid @RequestBody TransactionRequest req) {
        return ResponseEntity.ok(transactionService.create(req));
    }

    /** Sửa giao dịch */
    @PutMapping("/{id}")
    public ResponseEntity<TransactionResponse> update(@PathVariable Long id,
                                                       @Valid @RequestBody TransactionRequest req) {
        return ResponseEntity.ok(transactionService.update(id, req));
    }

    /**
     * Danh sách giao dịch: lọc theo mã CP, loại GD, ngày, tài khoản.
     * Phân trang và sắp xếp qua Spring Pageable.
     *
     * Ví dụ: GET /transactions?tickerSymbol=VNM&trade=BUY&page=0&size=20&sort=tradingDate,desc
     */
    @GetMapping
    public ResponseEntity<Page<TransactionResponse>> list(
            @RequestParam(required = false) String tickerSymbol,
            @RequestParam(required = false) TradeType trade,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Long accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "tradingDate,desc") String sort) {


        String[] sortParts = sort.split(",");
        Sort springSort = Sort.by(
                sortParts.length > 1 && "asc".equalsIgnoreCase(sortParts[1])
                        ? Sort.Direction.ASC : Sort.Direction.DESC,
                sortParts[0]);
        Pageable pageable = PageRequest.of(page, size, springSort);

        int debugController = 1;
        System.out.println("========== BEFORE CALL SERVICE ==========");

        return ResponseEntity.ok(transactionService.search(
                tickerSymbol, trade, fromDate, toDate, accountId, pageable));
    }

    /** Import giao dịch từ file Excel */
    @PostMapping("/import")
    public ResponseEntity<List<TransactionResponse>> importExcel(
            @RequestParam Long accountId,
            @RequestParam MultipartFile file) throws IOException {
        return ResponseEntity.ok(transactionService.importFromExcel(file, accountId));
    }

    @PostMapping("/import/stock-history")
    public ResponseEntity<List<TransactionResponse>> importStockHistory(
            @RequestParam Long accountId,
            @RequestParam MultipartFile file) throws IOException {
        return ResponseEntity.ok(transactionService.importStockTransactionHistoryExcel(file, accountId));
    }

    @PostMapping("/import/fund-history")
    public ResponseEntity<List<TransactionResponse>> importFundHistory(
            @RequestParam Long accountId,
            @RequestParam MultipartFile file) throws IOException {
        return ResponseEntity.ok(transactionService.importFundStatementExcel(file, accountId));
    }

    @PostMapping("/import/stock-statement")
    public ResponseEntity<List<TransactionResponse>> importStockStatement(
            @RequestParam Long accountId,
            @RequestParam MultipartFile file) throws IOException {
        return ResponseEntity.ok(transactionService.importStockStatementExcel(file, accountId));
    }
}
