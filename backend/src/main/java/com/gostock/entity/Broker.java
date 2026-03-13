package com.gostock.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Broker – Công ty chứng khoán / sàn giao dịch.
 * Ví dụ: SSI, TCBS, VPS, HSC…
 */
@Entity
@Table(name = "broker")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Broker {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;        // Mã broker (SSI, TCBS…)

    @Column(nullable = false)
    private String name;        // Tên công ty CK

    /**
     * Phí mua bán mặc định (tính theo %). 
     * VD: 0.15 = 0.15%/giao dịch. Có thể ghi đè ở từng giao dịch.
     */
    @Column(precision = 10, scale = 4)
    private BigDecimal defaultFeeRate;

    private String website;
    private boolean active = true;
}
