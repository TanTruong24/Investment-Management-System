package com.gostock.controller;

import com.gostock.entity.Broker;
import com.gostock.entity.Ticker;
import com.gostock.entity.Account;
import com.gostock.repository.BrokerRepository;
import com.gostock.repository.TickerRepository;
import com.gostock.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/ref")
@RequiredArgsConstructor
public class ReferenceDataController {

    private final TickerRepository tickerRepo;
    private final BrokerRepository brokerRepo;
    private final AccountRepository accountRepo;

    // ── Tickers ──────────────────────────────────────────────────────────
    @GetMapping("/tickers")
    public List<Ticker> getTickers() { return tickerRepo.findAll(); }

    @PostMapping("/tickers")
    public ResponseEntity<Ticker> createTicker(@RequestBody Ticker ticker) {
        return ResponseEntity.ok(tickerRepo.save(ticker));
    }

    @PutMapping("/tickers/{id}")
    public ResponseEntity<Ticker> updateTicker(@PathVariable Long id, @RequestBody Ticker req) {
        Ticker t = tickerRepo.findById(id).orElseThrow();
        t.setSymbol(req.getSymbol());
        t.setName(req.getName());
        t.setType(req.getType());
        t.setExchange(req.getExchange());
        t.setIndustry(req.getIndustry());
        return ResponseEntity.ok(tickerRepo.save(t));
    }

    // ── Brokers ──────────────────────────────────────────────────────────
    @GetMapping("/brokers")
    public List<Broker> getBrokers() { return brokerRepo.findAll(); }

    @PostMapping("/brokers")
    public ResponseEntity<Broker> createBroker(@RequestBody Broker broker) {
        return ResponseEntity.ok(brokerRepo.save(broker));
    }

    @PutMapping("/brokers/{id}")
    public ResponseEntity<Broker> updateBroker(@PathVariable Long id, @RequestBody Broker req) {
        Broker b = brokerRepo.findById(id).orElseThrow();
        b.setName(req.getName());
        b.setDefaultFeeRate(req.getDefaultFeeRate());
        b.setWebsite(req.getWebsite());
        b.setActive(req.isActive());
        return ResponseEntity.ok(brokerRepo.save(b));
    }

    // ── Accounts ─────────────────────────────────────────────────────────
    @GetMapping("/accounts")
    public List<Account> getAccounts() { return accountRepo.findAll(); }

    @PostMapping("/accounts")
    public ResponseEntity<Account> createAccount(@RequestBody Account account) {
        String accountNumber = account.getAccountNumber() != null ? account.getAccountNumber().trim() : null;
        String name = account.getName() != null ? account.getName().trim() : null;

        if (accountNumber == null || accountNumber.isBlank()) {
            throw new IllegalArgumentException("Số tài khoản không được để trống.");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Tên tài khoản không được để trống.");
        }
        if (accountRepo.existsByAccountNumberIgnoreCase(accountNumber)) {
            throw new IllegalArgumentException("Số tài khoản đã tồn tại: " + accountNumber);
        }

        Long brokerId = account.getBroker() != null ? account.getBroker().getId() : null;
        if (brokerId == null) {
            throw new IllegalArgumentException("Vui lòng chọn broker hợp lệ.");
        }

        Broker broker = brokerRepo.findById(brokerId)
                .orElseThrow(() -> new IllegalArgumentException("Broker không tồn tại: " + brokerId));

        Account toSave = Account.builder()
                .accountNumber(accountNumber)
                .name(name)
                .broker(broker)
                .cashBalance(account.getCashBalance() != null ? account.getCashBalance() : BigDecimal.ZERO)
                .purchasingPower(account.getPurchasingPower() != null ? account.getPurchasingPower() : BigDecimal.ZERO)
                .availableForWithdrawal(account.getAvailableForWithdrawal() != null ? account.getAvailableForWithdrawal() : BigDecimal.ZERO)
                .active(account.isActive())
                .build();

        return ResponseEntity.ok(accountRepo.save(toSave));
    }

    @PutMapping("/accounts/{id}")
    public ResponseEntity<Account> updateAccount(@PathVariable Long id, @RequestBody Account req) {
        Account a = accountRepo.findById(id).orElseThrow();

        String accountNumber = req.getAccountNumber() != null ? req.getAccountNumber().trim() : null;
        String name = req.getName() != null ? req.getName().trim() : null;
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new IllegalArgumentException("Số tài khoản không được để trống.");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Tên tài khoản không được để trống.");
        }
        if (accountRepo.existsByAccountNumberIgnoreCaseAndIdNot(accountNumber, id)) {
            throw new IllegalArgumentException("Số tài khoản đã tồn tại: " + accountNumber);
        }

        a.setAccountNumber(accountNumber);
        a.setName(name);
        if (req.getBroker() != null && req.getBroker().getId() != null) {
            Broker broker = brokerRepo.findById(req.getBroker().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Broker không tồn tại: " + req.getBroker().getId()));
            a.setBroker(broker);
        }
        a.setActive(req.isActive());
        return ResponseEntity.ok(accountRepo.save(a));
    }
}
