package com.gostock.controller;

import com.gostock.dto.CashFlowRequest;
import com.gostock.dto.response.SuccessResponse;
import com.gostock.dto.response.base.ApiResponse;
import com.gostock.service.contract.CashFlowServiceContract;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/cash-flows")
@RequiredArgsConstructor
public class CashFlowController {

    private final CashFlowServiceContract cashFlowService;

    /** Nạp tiền hoặc rút tiền */
    @PostMapping
    public ResponseEntity<? extends ApiResponse<?>> create(@Valid @RequestBody CashFlowRequest req) {
        return ResponseEntity.ok(new SuccessResponse<>(cashFlowService.create(req)));
    }

    /** Lịch sử nạp/rút tiền của tài khoản */
    @GetMapping
    public ResponseEntity<? extends ApiResponse<?>> listByAccount(@RequestParam Long accountId) {
        return ResponseEntity.ok(new SuccessResponse<>(cashFlowService.listByAccount(accountId)));
    }

    /** Import sao kê tiền từ file Excel */
    @PostMapping("/import")
    public ResponseEntity<? extends ApiResponse<?>> importExcel(
            @RequestParam Long accountId,
            @RequestParam MultipartFile file) throws IOException {
        return ResponseEntity.ok(new SuccessResponse<>(cashFlowService.importFromExcel(file, accountId)));
    }

    @PostMapping("/import/cash-statement")
    public ResponseEntity<? extends ApiResponse<?>> importCashStatement(
            @RequestParam Long accountId,
            @RequestParam MultipartFile file) throws IOException {
        return ResponseEntity.ok(new SuccessResponse<>(cashFlowService.importFromExcel(file, accountId)));
    }
}
