package com.gostock.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/** Một mục phân bổ danh mục (biểu đồ tròn) */
@Data @Builder
public class AllocationItem {
    private String label;           // Tên mã / nhóm
    private BigDecimal value;       // Giá trị thị trường
    private BigDecimal percentage;  // % trong tổng danh mục
}
