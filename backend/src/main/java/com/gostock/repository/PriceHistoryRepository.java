package com.gostock.repository;

import com.gostock.entity.PriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Long> {

    /** Lấy giá mới nhất của một mã */
    @Query("SELECT p FROM PriceHistory p WHERE p.ticker.id = :tickerId ORDER BY p.priceDate DESC LIMIT 1")
    Optional<PriceHistory> findLatestByTickerId(@Param("tickerId") Long tickerId);
}
