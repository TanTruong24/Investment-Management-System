package com.gostock.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * PriceHistory – Lịch sử giá thị trường của từng mã.
 * Được cập nhật thủ công hoặc qua crawl.
 */
@Entity
@Table(name = "price_history",
       uniqueConstraints = @UniqueConstraint(columnNames = {"ticker_id", "price_date"}),
       indexes = {
           @Index(name = "idx_ph_ticker_date", columnList = "ticker_id, price_date")
       })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PriceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticker_id")
    private Ticker ticker;

    @Column(nullable = false)
    private LocalDate priceDate;

    @Column(nullable = false, precision = 20, scale = 2)
    private BigDecimal closePrice;          // Giá đóng cửa

    @Column(precision = 20, scale = 2)
    private BigDecimal openPrice;

    @Column(precision = 20, scale = 2)
    private BigDecimal highPrice;

    @Column(precision = 20, scale = 2)
    private BigDecimal lowPrice;

    private Long volume;                    // KL khớp trong ngày

    /** "MANUAL" hoặc "CRAWL" */
    @Column(length = 10)
    private String source;
}
