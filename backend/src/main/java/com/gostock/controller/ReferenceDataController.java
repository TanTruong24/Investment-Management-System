package com.gostock.controller;

import com.gostock.dto.response.SuccessResponse;
import com.gostock.dto.response.base.ApiResponse;
import com.gostock.entity.Broker;
import com.gostock.entity.Ticker;
import com.gostock.entity.Account;
import com.gostock.service.contract.ReferenceDataServiceContract;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ref")
@RequiredArgsConstructor
public class ReferenceDataController {

    private final ReferenceDataServiceContract referenceDataService;

    // ── Tickers ──────────────────────────────────────────────────────────
    @GetMapping("/tickers")
    public ResponseEntity<? extends ApiResponse<?>> getTickers() {
        return ResponseEntity.ok(new SuccessResponse<>(referenceDataService.getTickers()));
    }

    @PostMapping("/tickers")
    public ResponseEntity<? extends ApiResponse<?>> createTicker(@RequestBody Ticker ticker) {
        return ResponseEntity.ok(new SuccessResponse<>(referenceDataService.createTicker(ticker)));
    }

    @PutMapping("/tickers/{id}")
    public ResponseEntity<? extends ApiResponse<?>> updateTicker(@PathVariable Long id, @RequestBody Ticker req) {
        return ResponseEntity.ok(new SuccessResponse<>(referenceDataService.updateTicker(id, req)));
    }

    // ── Brokers ──────────────────────────────────────────────────────────
    @GetMapping("/brokers")
    public ResponseEntity<? extends ApiResponse<?>> getBrokers() {
        return ResponseEntity.ok(new SuccessResponse<>(referenceDataService.getBrokers()));
    }

    @PostMapping("/brokers")
    public ResponseEntity<? extends ApiResponse<?>> createBroker(@RequestBody Broker broker) {
        return ResponseEntity.ok(new SuccessResponse<>(referenceDataService.createBroker(broker)));
    }

    @PutMapping("/brokers/{id}")
    public ResponseEntity<? extends ApiResponse<?>> updateBroker(@PathVariable Long id, @RequestBody Broker req) {
        return ResponseEntity.ok(new SuccessResponse<>(referenceDataService.updateBroker(id, req)));
    }

    // ── Accounts ─────────────────────────────────────────────────────────
    @GetMapping("/accounts")
    public ResponseEntity<? extends ApiResponse<?>> getAccounts() {
        return ResponseEntity.ok(new SuccessResponse<>(referenceDataService.getAccounts()));
    }

    @PostMapping("/accounts")
    public ResponseEntity<? extends ApiResponse<?>> createAccount(@RequestBody Account account) {
        return ResponseEntity.ok(new SuccessResponse<>(referenceDataService.createAccount(account)));
    }

    @PutMapping("/accounts/{id}")
    public ResponseEntity<? extends ApiResponse<?>> updateAccount(@PathVariable Long id, @RequestBody Account req) {
        return ResponseEntity.ok(new SuccessResponse<>(referenceDataService.updateAccount(id, req)));
    }
}
