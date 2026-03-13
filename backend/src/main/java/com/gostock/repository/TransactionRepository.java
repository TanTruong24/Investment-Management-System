package com.gostock.repository;

import com.gostock.entity.Transaction;
import com.gostock.entity.enums.TradeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TransactionRepository
        extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    @Query("""
        SELECT t FROM Transaction t
        JOIN FETCH t.ticker
        JOIN FETCH t.account
        WHERE (:tickerSymbol IS NULL OR t.ticker.symbol = :tickerSymbol)
          AND (:trade       IS NULL OR t.trade = :trade)
          AND (:fromDate    IS NULL OR t.tradingDate >= :fromDate)
          AND (:toDate      IS NULL OR t.tradingDate <= :toDate)
          AND (:accountId   IS NULL OR t.account.id = :accountId)
        ORDER BY t.tradingDate DESC, t.id DESC
        """)
    List<Transaction> search(
            @Param("tickerSymbol") String tickerSymbol,
            @Param("trade") TradeType trade,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("accountId") Long accountId);

    boolean existsByOrderNoAndAccount_Id(String orderNo, Long accountId);
}
