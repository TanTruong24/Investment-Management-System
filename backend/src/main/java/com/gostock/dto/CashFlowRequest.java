package com.gostock.dto;

import com.gostock.entity.enums.CashFlowType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CashFlowRequest {
    @NotNull
    private Long accountId;

    @NotNull
    private CashFlowType type;      // DEPOSIT / WITHDRAW

    @NotNull @Positive
    private BigDecimal amount;

    @NotNull
    private LocalDate flowDate;

    private String note;
}
