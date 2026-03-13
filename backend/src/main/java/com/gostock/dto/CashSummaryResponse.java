package com.gostock.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/** Tóm tắt tiền mặt của một tài khoản */
@Data @Builder
public class CashSummaryResponse {
    private Long accountId;
    private String accountNumber;
    private String brokerCode;

    private BigDecimal totalDeposit;            // Tổng đã nạp
    private BigDecimal totalWithdraw;           // Tổng đã rút
    private BigDecimal netCashIn;               // Tiền ròng đã đưa vào = deposit - withdraw
    private BigDecimal cashBalance;             // Số dư tiền mặt hiện tại
    private BigDecimal purchasingPower;         // Sức mua
    private BigDecimal availableForWithdrawal;  // Tiền có thể rút
}
