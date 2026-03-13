package com.gostock.entity;

import com.gostock.entity.enums.InstrumentType;
import com.gostock.entity.enums.StockExchange;
import jakarta.persistence.*;
import lombok.*;

/**
 * Ticker – Mã chứng khoán / công cụ tài chính.
 * Ví dụ: VNM (cổ phiếu), VNINDEX (chỉ số), DCDS (CCQ).
 */
@Entity
@Table(name = "ticker")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Ticker {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String symbol;          // Mã CP / ký hiệu

    @Column(nullable = false)
    private String name;            // Tên đầy đủ

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InstrumentType type;    // Loại tài sản

    @Enumerated(EnumType.STRING)
    private StockExchange exchange;  // Sàn niêm yết (có thể null với quỹ, vàng)

    private String industry;        // Ngành
    private String description;
    private boolean active = true;
}
