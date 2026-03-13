package com.gostock.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PriceUpdateRequest {
    @NotBlank
    private String tickerSymbol;

    @NotNull
    private LocalDate priceDate;

    @NotNull @Positive
    private BigDecimal closePrice;

    private BigDecimal openPrice;
    private BigDecimal highPrice;
    private BigDecimal lowPrice;
    private Long volume;

    /** "MANUAL" hoặc "CRAWL" */
    private String source = "MANUAL";
}
