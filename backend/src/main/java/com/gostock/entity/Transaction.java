package com.gostock.entity;

import com.gostock.entity.enums.*;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Transaction – Giao dịch mua/bán chứng khoán.
 * Ánh xạ đầy đủ các trường từ bảng glossary trong sơ đồ.
 */
@Entity
@Table(name = "transaction",
       indexes = {
           @Index(name = "idx_tx_ticker", columnList = "ticker_id"),
           @Index(name = "idx_tx_trading_date", columnList = "trading_date"),
           @Index(name = "idx_tx_account", columnList = "account_id")
       })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Thông tin lệnh ──────────────────────────────────────────────────

    /** Số hiệu lệnh do sàn/broker cấp */
    @Column(length = 50)
    private String orderNo;

    @Column(nullable = false)
    private LocalDate tradingDate;          // Ngày GD

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TradeType trade;                // BUY / SELL

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticker_id")
    private Ticker ticker;                  // Mã CP

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id")
    private Account account;                // Tài khoản

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private StockExchange stockExchange;    // Sàn GD CK (HOSE, HNX…)

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private OrderType orderType;            // Loại lệnh (thường, phái sinh)

    @Column(length = 20)
    private String channel;                 // Kênh GD (online, phone…)

    // ── Số lượng & giá ─────────────────────────────────────────────────

    @Column(nullable = false)
    private Long volume;                    // Khối lượng đặt

    @Column(nullable = false, precision = 20, scale = 2)
    private BigDecimal orderPrice;          // Giá đặt (VNĐ)

    private Long matchedVolume;             // KL khớp

    @Column(precision = 20, scale = 2)
    private BigDecimal matchedPrice;        // Giá khớp (VNĐ)

    @Column(precision = 20, scale = 2)
    private BigDecimal matchedValue;        // Giá trị khớp (VNĐ)

    // ── Phí & thuế ──────────────────────────────────────────────────────

    /**
     * Phí giao dịch (VNĐ). Được tính từ fee rate của broker.
     * = matchedVolume × matchedPrice × feeRate
     */
    @Column(precision = 20, scale = 2)
    private BigDecimal fee;

    /**
     * Thuế (VNĐ). Theo luật VN: 0.1% giá trị giao dịch khi bán.
     * = matchedVolume × matchedPrice × 0.001
     */
    @Column(precision = 20, scale = 2)
    private BigDecimal tax;

    /**
     * Giá vốn (VNĐ) = tổng tiền thực chi cho lệnh mua (gồm phí, thuế).
     * Với lệnh bán: cost là giá vốn bình quân của CP đã bán.
     */
    @Column(precision = 20, scale = 2)
    private BigDecimal cost;

    /**
     * Lãi/lỗ đã thực hiện trên giao dịch này (chỉ có ý nghĩa với lệnh bán).
     * = doanh thu bán – giá vốn – phí – thuế
     */
    @Column(precision = 20, scale = 2)
    private BigDecimal returnAmount;        // Lãi lỗ (VNĐ)

    // ── Trạng thái ──────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status;       // Trạng thái

    private String note;                    // Ghi chú

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
