package com.gostock.repository;

import com.gostock.entity.Position;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PositionRepository extends JpaRepository<Position, Long> {

    Optional<Position> findByAccount_IdAndTicker_Id(Long accountId, Long tickerId);

    /** Danh sách vị thế đang có số dư > 0 */
    @Query("SELECT p FROM Position p JOIN FETCH p.ticker WHERE p.account.id = :accountId AND p.holdingVolume > 0")
    List<Position> findActiveByAccountId(@Param("accountId") Long accountId);

    List<Position> findByAccount_IdAndHoldingVolumeGreaterThan(Long accountId, Long volume);

    @Query("SELECT DISTINCT p.ticker.symbol FROM Position p WHERE p.holdingVolume > 0")
    List<String> findDistinctHeldTickerSymbols();

    @Query("SELECT DISTINCT p.ticker.symbol FROM Position p WHERE p.account.id = :accountId AND p.holdingVolume > 0")
    List<String> findDistinctHeldTickerSymbolsByAccountId(@Param("accountId") Long accountId);
}
