package com.gostock.repository;

import com.gostock.entity.CashFlow;
import com.gostock.entity.enums.CashFlowType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface CashFlowRepository extends JpaRepository<CashFlow, Long> {

    List<CashFlow> findByAccount_IdOrderByFlowDateDesc(Long accountId);

    boolean existsByAccount_IdAndTypeAndAmountAndFlowDateAndNote(
            Long accountId,
            CashFlowType type,
            BigDecimal amount,
            java.time.LocalDate flowDate,
            String note);

    @Query("SELECT COALESCE(SUM(c.amount), 0) FROM CashFlow c WHERE c.account.id = :accountId AND c.type = :type")
    BigDecimal sumByAccountAndType(@Param("accountId") Long accountId,
                                   @Param("type") CashFlowType type);
}
