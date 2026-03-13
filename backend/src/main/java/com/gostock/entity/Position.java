package com.gostock.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Position – Trạng thái nắm giữ tài sản tại một tài khoản.
 * Được tính toán và cập nhật mỗi khi có giao dịch mua/bán.
 *
 * Unrealized PnL = (currentPrice - avgCost) × holdingVolume
 */
@Entity
@Table(name = "position",
       uniqueConstraints = @UniqueConstraint(columnNames = {"account_id", "ticker_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Position {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id")
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticker_id")
    private Ticker ticker;

    /** Khối lượng đang nắm giữ */
    @Column(nullable = false)
    private Long holdingVolume = 0L;

    /** Giá vốn bình quân (VNĐ/CP) */
    @Column(precision = 20, scale = 4)
    private BigDecimal avgCost = BigDecimal.ZERO;

    /** Giá thị trường hiện tại (cập nhật từ PriceHistory) */
    @Column(precision = 20, scale = 2)
    private BigDecimal currentPrice = BigDecimal.ZERO;

    /** Lãi lỗ chưa thực hiện (VNĐ) */
    @Column(name = "unrealized_pn_l", precision = 20, scale = 2)
    private BigDecimal unrealizedPnL = BigDecimal.ZERO;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
