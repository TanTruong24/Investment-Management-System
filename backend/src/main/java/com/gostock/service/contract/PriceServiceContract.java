package com.gostock.service.contract;

import com.gostock.dto.PriceUpdateRequest;
import com.gostock.entity.PriceHistory;

public interface PriceServiceContract {
    PriceHistory updatePrice(PriceUpdateRequest req);
}
