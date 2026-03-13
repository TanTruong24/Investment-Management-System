package com.gostock.entity;

import com.gostock.entity.enums.CashFlowType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * CashFlow – Lịch sử nạp/rút tiền vào tài khoản.
 */
@Entity
@Table(name = "cash_flow",
       indexes = @Index(name = "idx_cf_account", columnList = "account_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CashFlow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id")
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private CashFlowType type;          // DEPOSIT / WITHDRAW

    @Column(nullable = false, precision = 20, scale = 2)
    private BigDecimal amount;          // Số tiền

    @Column(nullable = false)
    private LocalDate flowDate;         // Ngày giao dịch tiền

    private String note;                // Ghi chú

    @CreationTimestamp
    private LocalDateTime createdAt;
}
