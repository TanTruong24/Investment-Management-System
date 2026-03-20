package com.gostock.service;

import com.gostock.dto.PriceUpdateRequest;
import com.gostock.entity.PriceHistory;
import com.gostock.entity.Position;
import com.gostock.entity.Ticker;
import com.gostock.repository.PositionRepository;
import com.gostock.repository.PriceHistoryRepository;
import com.gostock.repository.TickerRepository;
import com.gostock.service.contract.PriceServiceContract;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PriceService implements PriceServiceContract {

    private final PriceHistoryRepository priceHistoryRepo;
    private final TickerRepository tickerRepo;
    private final PositionRepository positionRepo;

    /**
     * Cập nhật/lưu giá mới nhất và đồng bộ vào Position (unrealized PnL).
     */
    @Override
    public PriceHistory updatePrice(PriceUpdateRequest req) {
        Ticker ticker = tickerRepo.findBySymbolIgnoreCase(req.getTickerSymbol())
                .orElseThrow(() -> new EntityNotFoundException("Ticker not found: " + req.getTickerSymbol()));

        PriceHistory price = PriceHistory.builder()
                .ticker(ticker)
                .priceDate(req.getPriceDate())
                .closePrice(req.getClosePrice())
                .openPrice(req.getOpenPrice())
                .highPrice(req.getHighPrice())
                .lowPrice(req.getLowPrice())
                .volume(req.getVolume())
                .source(req.getSource())
                .build();

        PriceHistory saved = priceHistoryRepo.save(price);

        // Đồng bộ unrealized PnL cho tất cả Position của mã này
        List<Position> positions = positionRepo
                .findByAccount_IdAndHoldingVolumeGreaterThan(null, 0L)
                .stream()
                .filter(p -> p.getTicker().getId().equals(ticker.getId()))
                .toList();

        // Dùng query riêng hiệu quả hơn
        resyncPositionPriceForTicker(ticker.getId(), req.getClosePrice());

        return saved;
    }

    private void resyncPositionPriceForTicker(Long tickerId, BigDecimal newPrice) {
        List<Position> positions = positionRepo.findAll().stream()
                .filter(p -> p.getTicker().getId().equals(tickerId) && p.getHoldingVolume() > 0)
                .toList();

        for (Position pos : positions) {
            pos.setCurrentPrice(newPrice);
            BigDecimal marketValue = newPrice.multiply(BigDecimal.valueOf(pos.getHoldingVolume()));
            BigDecimal invested = pos.getAvgCost().multiply(BigDecimal.valueOf(pos.getHoldingVolume()));
            pos.setUnrealizedPnL(marketValue.subtract(invested).setScale(2, RoundingMode.HALF_UP));
        }
        positionRepo.saveAll(positions);
    }
}
