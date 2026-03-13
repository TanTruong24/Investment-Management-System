package com.gostock.repository;

import com.gostock.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccountRepository extends JpaRepository<Account, Long> {
    List<Account> findByBroker_Id(Long brokerId);

    boolean existsByAccountNumberIgnoreCase(String accountNumber);

    boolean existsByAccountNumberIgnoreCaseAndIdNot(String accountNumber, Long id);
}
