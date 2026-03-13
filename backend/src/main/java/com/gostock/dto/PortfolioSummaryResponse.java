package com.gostock.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/** Tóm tắt hiệu suất danh mục */
@Data @Builder
public class PortfolioSummaryResponse {

    private BigDecimal totalInvested;       // Tổng vốn đầu tư
    private BigDecimal currentValue;        // Giá trị hiện tại
    private BigDecimal unrealizedPnL;       // Lãi/lỗ chưa thực hiện
    private BigDecimal realizedPnL;         // Lãi/lỗ đã thực hiện
    private BigDecimal totalPnL;            // Tổng lãi lỗ
    private BigDecimal returnRate;          // Tỷ suất lợi nhuận (%)

    private List<HoldingResponse> holdings; // Danh sách tài sản đang giữ
    private List<AllocationItem> allocation; // Phân bổ danh mục
}
