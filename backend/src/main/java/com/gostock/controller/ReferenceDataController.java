package com.gostock.controller;

import com.gostock.entity.Broker;
import com.gostock.entity.Ticker;
import com.gostock.entity.Account;
import com.gostock.service.contract.ReferenceDataServiceContract;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ref")
@RequiredArgsConstructor
public class ReferenceDataController {

    private final ReferenceDataServiceContract referenceDataService;

    // ── Tickers ──────────────────────────────────────────────────────────
    @GetMapping("/tickers")
    public List<Ticker> getTickers() { return referenceDataService.getTickers(); }

    @PostMapping("/tickers")
    public ResponseEntity<Ticker> createTicker(@RequestBody Ticker ticker) {
        return ResponseEntity.ok(referenceDataService.createTicker(ticker));
    }

    @PutMapping("/tickers/{id}")
    public ResponseEntity<Ticker> updateTicker(@PathVariable Long id, @RequestBody Ticker req) {
        return ResponseEntity.ok(referenceDataService.updateTicker(id, req));
    }

    // ── Brokers ──────────────────────────────────────────────────────────
    @GetMapping("/brokers")
    public List<Broker> getBrokers() { return referenceDataService.getBrokers(); }

    @PostMapping("/brokers")
    public ResponseEntity<Broker> createBroker(@RequestBody Broker broker) {
        return ResponseEntity.ok(referenceDataService.createBroker(broker));
    }

    @PutMapping("/brokers/{id}")
    public ResponseEntity<Broker> updateBroker(@PathVariable Long id, @RequestBody Broker req) {
        return ResponseEntity.ok(referenceDataService.updateBroker(id, req));
    }

    // ── Accounts ─────────────────────────────────────────────────────────
    @GetMapping("/accounts")
    public List<Account> getAccounts() { return referenceDataService.getAccounts(); }

    @PostMapping("/accounts")
    public ResponseEntity<Account> createAccount(@RequestBody Account account) {
        return ResponseEntity.ok(referenceDataService.createAccount(account));
    }

    @PutMapping("/accounts/{id}")
    public ResponseEntity<Account> updateAccount(@PathVariable Long id, @RequestBody Account req) {
        return ResponseEntity.ok(referenceDataService.updateAccount(id, req));
    }
}
