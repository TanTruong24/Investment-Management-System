package com.gostock.controller;

import com.gostock.dto.PriceUpdateRequest;
import com.gostock.entity.PriceHistory;
import com.gostock.service.contract.PriceServiceContract;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/prices")
@RequiredArgsConstructor
public class PriceController {

    private final PriceServiceContract priceService;

    /** Cập nhật giá thị trường (thủ công hoặc từ crawler) */
    @PostMapping
    public ResponseEntity<PriceHistory> updatePrice(@Valid @RequestBody PriceUpdateRequest req) {
        return ResponseEntity.ok(priceService.updatePrice(req));
    }
}
