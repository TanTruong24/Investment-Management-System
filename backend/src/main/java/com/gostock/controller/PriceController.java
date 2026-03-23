package com.gostock.controller;

import com.gostock.dto.PriceUpdateRequest;
import com.gostock.dto.response.SuccessResponse;
import com.gostock.dto.response.base.ApiResponse;
import com.gostock.service.contract.PriceServiceContract;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/prices")
@RequiredArgsConstructor
public class PriceController {

    private final PriceServiceContract priceService;

    /** Cập nhật giá thị trường (thủ công hoặc từ crawler) */
    @PostMapping
    public ResponseEntity<? extends ApiResponse<?>> updatePrice(@Valid @RequestBody PriceUpdateRequest req) {
        return ResponseEntity.ok(new SuccessResponse<>(priceService.updatePrice(req)));
    }

    /**
     * Crawl giá mới nhất từ Vietstock cho các mã đang nắm giữ.
     * accountId null => cập nhật tất cả mã đang giữ của mọi tài khoản.
     */
    @PostMapping("/refresh-held")
    public ResponseEntity<? extends ApiResponse<?>> refreshHeld(@RequestParam(required = false) Long accountId) {
        var updated = priceService.refreshHeldTickerPrices(accountId);
        var items = updated.stream().map(p -> Map.of(
            "tickerSymbol", p.getTicker().getSymbol(),
            "priceDate", p.getPriceDate(),
            "closePrice", p.getClosePrice(),
            "source", p.getSource()
        )).toList();
        return ResponseEntity.ok(new SuccessResponse<>(Map.of(
            "updatedCount", items.size(),
            "items", items)));
    }
}
