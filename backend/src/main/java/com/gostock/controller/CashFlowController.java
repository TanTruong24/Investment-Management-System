package com.gostock.controller;

import com.gostock.dto.CashFlowRequest;
import com.gostock.entity.CashFlow;
import com.gostock.service.CashFlowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/cash-flows")
@RequiredArgsConstructor
public class CashFlowController {

    private final CashFlowService cashFlowService;

    /** Nạp tiền hoặc rút tiền */
    @PostMapping
    public ResponseEntity<CashFlow> create(@Valid @RequestBody CashFlowRequest req) {
        return ResponseEntity.ok(cashFlowService.create(req));
    }

    /** Lịch sử nạp/rút tiền của tài khoản */
    @GetMapping
    public ResponseEntity<List<CashFlow>> listByAccount(@RequestParam Long accountId) {
        return ResponseEntity.ok(cashFlowService.listByAccount(accountId));
    }

    /** Import sao kê tiền từ file Excel */
    @PostMapping("/import")
    public ResponseEntity<List<CashFlow>> importExcel(
            @RequestParam Long accountId,
            @RequestParam MultipartFile file) throws IOException {
        return ResponseEntity.ok(cashFlowService.importFromExcel(file, accountId));
    }

    @PostMapping("/import/cash-statement")
    public ResponseEntity<List<CashFlow>> importCashStatement(
            @RequestParam Long accountId,
            @RequestParam MultipartFile file) throws IOException {
        return ResponseEntity.ok(cashFlowService.importFromExcel(file, accountId));
    }
}
