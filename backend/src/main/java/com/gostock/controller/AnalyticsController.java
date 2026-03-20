package com.gostock.controller;

import com.gostock.dto.*;
import com.gostock.service.contract.AnalyticsServiceContract;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsServiceContract analyticsService;

    /** Tổng quan danh mục: holdings, PnL, phân bổ, hiệu suất */
    @GetMapping("/portfolio/{accountId}")
    public ResponseEntity<PortfolioSummaryResponse> portfolio(@PathVariable Long accountId) {
        return ResponseEntity.ok(analyticsService.getSummary(accountId));
    }

    /** Tóm tắt tiền mặt */
    @GetMapping("/cash/{accountId}")
    public ResponseEntity<CashSummaryResponse> cashSummary(@PathVariable Long accountId) {
        return ResponseEntity.ok(analyticsService.getCashSummary(accountId));
    }
}
