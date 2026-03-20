package com.gostock.service.contract;

import com.gostock.dto.CashSummaryResponse;
import com.gostock.dto.PortfolioSummaryResponse;

public interface AnalyticsServiceContract {
    PortfolioSummaryResponse getSummary(Long accountId);

    CashSummaryResponse getCashSummary(Long accountId);
}
