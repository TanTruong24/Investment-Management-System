package com.gostock.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Account – Tài khoản giao dịch tại một broker.
 * Một người có thể có nhiều tài khoản ở các CTCK khác nhau.
 */
@Entity
@Table(name = "account")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String accountNumber;   // Số tài khoản

    @Column(nullable = false)
    private String name;            // Tên hiển thị

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "broker_id")
    private Broker broker;

    /** Số dư tiền mặt hiện tại trong tài khoản */
    @Column(precision = 20, scale = 2)
    private BigDecimal cashBalance = BigDecimal.ZERO;

    /** Sức mua (tiền mặt + margin nếu có) */
    @Column(precision = 20, scale = 2)
    private BigDecimal purchasingPower = BigDecimal.ZERO;

    /** Số tiền thực tế có thể rút (sau khi trừ lệnh đang chờ xử lý) */
    @Column(precision = 20, scale = 2)
    private BigDecimal availableForWithdrawal = BigDecimal.ZERO;

    private boolean active = true;
}
