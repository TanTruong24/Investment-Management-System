package com.gostock.controller;

import com.gostock.dto.response.SuccessResponse;
import com.gostock.dto.response.base.ApiResponse;
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
    public ResponseEntity<? extends ApiResponse<?>> portfolio(@PathVariable Long accountId) {
        return ResponseEntity.ok(new SuccessResponse<>(analyticsService.getSummary(accountId)));
    }

    /** Tóm tắt tiền mặt */
    @GetMapping("/cash/{accountId}")
    public ResponseEntity<? extends ApiResponse<?>> cashSummary(@PathVariable Long accountId) {
        return ResponseEntity.ok(new SuccessResponse<>(analyticsService.getCashSummary(accountId)));
    }
}
