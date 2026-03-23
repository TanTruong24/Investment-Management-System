package com.gostock.dto;

import com.gostock.entity.enums.*;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/** DTO nhận request tạo / sửa giao dịch */
@Data
public class TransactionRequest {

    @NotNull
    private Long accountId;

    @NotBlank
    private String tickerSymbol;        // Mã CP (FE gõ vào)

    @NotNull
    private LocalDate tradingDate;

    @NotNull
    private TradeType trade;            // BUY / SELL

    @NotNull @Positive
    private Long volume;                // Khối lượng đặt

    @NotNull @Positive
    private BigDecimal orderPrice;      // Giá đặt (VNĐ)

    private Long matchedVolume;         // KL khớp (có thể null nếu chưa khớp)
    private BigDecimal matchedPrice;    // Giá khớp
    private BigDecimal matchedValue;    // Giá trị khớp

    private StockExchange stockExchange;
    private OrderType orderType;
    private String channel;
    private String orderNo;
    private String note;

    // Fee & tax: nếu FE không truyền, backend tự tính
    private BigDecimal fee;
    private BigDecimal tax;
}
