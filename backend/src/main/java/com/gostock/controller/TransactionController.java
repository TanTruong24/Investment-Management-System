package com.gostock.controller;

import com.gostock.dto.*;
import com.gostock.dto.response.SuccessResponse;
import com.gostock.dto.response.base.ApiResponse;
import com.gostock.entity.enums.TradeType;
import com.gostock.service.contract.TransactionServiceContract;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionServiceContract transactionService;

    /** Tạo giao dịch */
    @PostMapping
    public ResponseEntity<? extends ApiResponse<?>> create(@Valid @RequestBody TransactionRequest req) {
        return ResponseEntity.ok(new SuccessResponse<>(transactionService.create(req)));
    }

    /** Sửa giao dịch */
    @PutMapping("/{id}")
    public ResponseEntity<? extends ApiResponse<?>> update(@PathVariable Long id,
            @Valid @RequestBody TransactionRequest req) {
        return ResponseEntity.ok(new SuccessResponse<>(transactionService.update(id, req)));
    }

    /**
     * Danh sách giao dịch: lọc theo mã CP, loại GD, ngày, tài khoản.
     * Phân trang và sắp xếp qua Spring Pageable.
     *
     * Ví dụ: GET
     * /transactions?tickerSymbol=VNM&trade=BUY&page=0&size=20&sort=tradingDate,desc
     */
    @GetMapping
    public ResponseEntity<? extends ApiResponse<?>> list(
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
                        ? Sort.Direction.ASC
                        : Sort.Direction.DESC,
                sortParts[0]);
        Pageable pageable = PageRequest.of(page, size, springSort);

        return ResponseEntity.ok(transactionService.search(
                tickerSymbol, trade, fromDate, toDate, accountId, pageable));
    }

    /** Import giao dịch từ file Excel */
    @PostMapping("/import")
    public ResponseEntity<? extends ApiResponse<?>> importExcel(
            @RequestParam Long accountId,
            @RequestParam MultipartFile file) throws IOException {
        return ResponseEntity.ok(new SuccessResponse<>(transactionService.importFromExcel(file, accountId)));
    }

    @PostMapping("/import/stock-history")
    public ResponseEntity<? extends ApiResponse<?>> importStockHistory(
            @RequestParam Long accountId,
            @RequestParam MultipartFile file) throws IOException {
        return ResponseEntity
                .ok(new SuccessResponse<>(transactionService.importStockTransactionHistoryExcel(file, accountId)));
    }

    @PostMapping("/import/fund-history")
    public ResponseEntity<? extends ApiResponse<?>> importFundHistory(
            @RequestParam Long accountId,
            @RequestParam MultipartFile file) throws IOException {
        return ResponseEntity.ok(new SuccessResponse<>(transactionService.importFundStatementExcel(file, accountId)));
    }

    @PostMapping("/import/stock-statement")
    public ResponseEntity<? extends ApiResponse<?>> importStockStatement(
            @RequestParam Long accountId,
            @RequestParam MultipartFile file) throws IOException {
        return ResponseEntity.ok(new SuccessResponse<>(transactionService.importStockStatementExcel(file, accountId)));
    }
}
