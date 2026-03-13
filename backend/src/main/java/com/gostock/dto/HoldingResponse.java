package com.gostock.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/** Một dòng tài sản đang nắm giữ */
@Data @Builder
public class HoldingResponse {
    private String tickerSymbol;
    private String tickerName;
    private String exchange;
    private Long holdingVolume;
    private BigDecimal avgCost;         // Giá vốn bình quân
    private BigDecimal currentPrice;    // Giá thị trường hiện tại
    private BigDecimal marketValue;     // Giá trị thị trường = volume × currentPrice
    private BigDecimal unrealizedPnL;   // Lãi lỗ chưa thực hiện
    private BigDecimal unrealizedPnLPct;// Tỷ lệ lãi lỗ (%)
}
