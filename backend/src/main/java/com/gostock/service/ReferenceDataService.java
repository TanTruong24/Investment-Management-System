package com.gostock.service;

import com.gostock.entity.Account;
import com.gostock.entity.Broker;
import com.gostock.entity.Ticker;
import com.gostock.repository.AccountRepository;
import com.gostock.repository.BrokerRepository;
import com.gostock.repository.TickerRepository;
import com.gostock.service.contract.ReferenceDataServiceContract;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ReferenceDataService implements ReferenceDataServiceContract {

    private final TickerRepository tickerRepo;
    private final BrokerRepository brokerRepo;
    private final AccountRepository accountRepo;

    @Override
    @Transactional(readOnly = true)
    public List<Ticker> getTickers() {
        return tickerRepo.findAll();
    }

    @Override
    public Ticker createTicker(Ticker ticker) {
        return tickerRepo.save(ticker);
    }

    @Override
    public Ticker updateTicker(Long id, Ticker req) {
        Ticker ticker = tickerRepo.findById(id).orElseThrow();
        ticker.setSymbol(req.getSymbol());
        ticker.setName(req.getName());
        ticker.setType(req.getType());
        ticker.setExchange(req.getExchange());
        ticker.setIndustry(req.getIndustry());
        return tickerRepo.save(ticker);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Broker> getBrokers() {
        return brokerRepo.findAll();
    }

    @Override
    public Broker createBroker(Broker broker) {
        return brokerRepo.save(broker);
    }

    @Override
    public Broker updateBroker(Long id, Broker req) {
        Broker broker = brokerRepo.findById(id).orElseThrow();
        broker.setName(req.getName());
        broker.setDefaultFeeRate(req.getDefaultFeeRate());
        broker.setWebsite(req.getWebsite());
        broker.setActive(req.isActive());
        return brokerRepo.save(broker);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Account> getAccounts() {
        return accountRepo.findAll();
    }

    @Override
    public Account createAccount(Account account) {
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

        return accountRepo.save(toSave);
    }

    @Override
    public Account updateAccount(Long id, Account req) {
        Account account = accountRepo.findById(id).orElseThrow();

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

        account.setAccountNumber(accountNumber);
        account.setName(name);
        if (req.getBroker() != null && req.getBroker().getId() != null) {
            Broker broker = brokerRepo.findById(req.getBroker().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Broker không tồn tại: " + req.getBroker().getId()));
            account.setBroker(broker);
        }
        account.setActive(req.isActive());
        return accountRepo.save(account);
    }
}
