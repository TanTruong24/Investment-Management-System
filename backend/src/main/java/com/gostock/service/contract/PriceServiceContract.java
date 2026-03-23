package com.gostock.service.contract;

import com.gostock.dto.PriceUpdateRequest;
import com.gostock.entity.PriceHistory;

import java.util.List;

public interface PriceServiceContract {
    PriceHistory updatePrice(PriceUpdateRequest req);

    List<PriceHistory> refreshHeldTickerPrices(Long accountId);
}
