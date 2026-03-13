package com.gostock.repository;

import com.gostock.entity.Ticker;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TickerRepository extends JpaRepository<Ticker, Long> {
    Optional<Ticker> findBySymbolIgnoreCase(String symbol);
    boolean existsBySymbolIgnoreCase(String symbol);
}
