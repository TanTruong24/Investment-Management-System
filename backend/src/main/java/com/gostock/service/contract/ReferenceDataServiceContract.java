package com.gostock.service.contract;

import com.gostock.entity.Account;
import com.gostock.entity.Broker;
import com.gostock.entity.Ticker;

import java.util.List;

public interface ReferenceDataServiceContract {
    List<Ticker> getTickers();

    Ticker createTicker(Ticker ticker);

    Ticker updateTicker(Long id, Ticker req);

    List<Broker> getBrokers();

    Broker createBroker(Broker broker);

    Broker updateBroker(Long id, Broker req);

    List<Account> getAccounts();

    Account createAccount(Account account);

    Account updateAccount(Long id, Account req);
}
