package com.gostock.dto;

import com.gostock.entity.enums.*;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;


/** DTO trả về cho FE */
@Data 
@Builder
public class TransactionResponse {
    private Long id;
    private String orderNo;
    private LocalDate tradingDate;
    private TradeType trade;
    private String tickerSymbol;
    private String tickerName;
    private String accountName;
    private String brokerCode;
    private StockExchange stockExchange;
    private OrderType orderType;
    private String channel;
    private Long volume;
    private BigDecimal orderPrice;
    private Long matchedVolume;
    private BigDecimal matchedPrice;
    private BigDecimal matchedValue;
    private BigDecimal fee;
    private BigDecimal tax;
    private BigDecimal cost;
    private BigDecimal returnAmount;
    private TransactionStatus status;
    private String note;
    private LocalDateTime createdAt;
}
