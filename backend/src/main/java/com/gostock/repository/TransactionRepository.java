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

    @Query("""
        SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END
        FROM Transaction t
        WHERE t.account.id = :accountId
          AND t.orderNo = :orderNo
          AND t.tradingDate = :tradingDate
          AND t.trade = :trade
          AND t.ticker.symbol = :tickerSymbol
          AND t.matchedVolume = :matchedVolume
          AND t.matchedPrice = :matchedPrice
          AND t.matchedValue = :matchedValue
        """)
    boolean existsByImportSignature(
            @Param("accountId") Long accountId,
            @Param("orderNo") String orderNo,
            @Param("tradingDate") LocalDate tradingDate,
            @Param("trade") TradeType trade,
            @Param("tickerSymbol") String tickerSymbol,
            @Param("matchedVolume") Long matchedVolume,
            @Param("matchedPrice") java.math.BigDecimal matchedPrice,
            @Param("matchedValue") java.math.BigDecimal matchedValue);

    boolean existsByOrderNoAndAccount_Id(String orderNo, Long accountId);
}
