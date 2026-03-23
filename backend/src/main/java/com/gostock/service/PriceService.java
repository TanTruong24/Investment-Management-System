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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class PriceService implements PriceServiceContract {

    private static final Logger log = LoggerFactory.getLogger(PriceService.class);

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

        LocalDate priceDate = req.getPriceDate() != null ? req.getPriceDate() : LocalDate.now();
        PriceHistory saved = upsertPriceHistory(
            ticker,
            priceDate,
            req.getClosePrice(),
            req.getOpenPrice(),
            req.getHighPrice(),
            req.getLowPrice(),
            req.getVolume(),
            req.getSource());

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

    @Override
    public List<PriceHistory> refreshHeldTickerPrices(Long accountId) {
        List<String> symbols = accountId == null
                ? positionRepo.findDistinctHeldTickerSymbols()
                : positionRepo.findDistinctHeldTickerSymbolsByAccountId(accountId);

        if (symbols.isEmpty()) {
            return List.of();
        }

        List<PriceHistory> updated = new ArrayList<>();
        for (String symbol : symbols) {
            try {
                VietstockSnapshot snapshot = fetchLatestFromVietstock(symbol);
                Ticker ticker = tickerRepo.findBySymbolIgnoreCase(symbol)
                        .orElseThrow(() -> new EntityNotFoundException("Ticker not found: " + symbol));
                PriceHistory saved = upsertPriceHistory(
                        ticker,
                        snapshot.priceDate,
                        snapshot.closePrice,
                        null,
                        null,
                        null,
                        null,
                        "CRAWL");
                resyncPositionPriceForTicker(ticker.getId(), snapshot.closePrice);
                updated.add(saved);
            } catch (Exception ex) {
                log.warn("Failed to refresh price for {}: {}", symbol, ex.getMessage());
                // Bỏ qua mã lỗi để không chặn toàn bộ batch cập nhật giá.
            }
        }
        return updated;
    }

    private PriceHistory upsertPriceHistory(
            Ticker ticker,
            LocalDate priceDate,
            BigDecimal closePrice,
            BigDecimal openPrice,
            BigDecimal highPrice,
            BigDecimal lowPrice,
            Long volume,
            String source) {
        PriceHistory target = priceHistoryRepo.findByTicker_IdAndPriceDate(ticker.getId(), priceDate)
                .orElseGet(() -> PriceHistory.builder().ticker(ticker).priceDate(priceDate).build());

        target.setClosePrice(closePrice);
        target.setOpenPrice(openPrice);
        target.setHighPrice(highPrice);
        target.setLowPrice(lowPrice);
        target.setVolume(volume);
        target.setSource(source);
        return priceHistoryRepo.save(target);
    }

    private VietstockSnapshot fetchLatestFromVietstock(String symbol) {
        String url = "https://finance.vietstock.vn/" + symbol.toUpperCase(Locale.ROOT) + "/du-lieu-lich-su.htm";
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .timeout(15000)
                    .get();

            Elements cols = doc.select("#stock-transactions tbody tr:first-child td");
            // System.out.println(cols);
            if (cols.size() < 2) {
                throw new IllegalStateException("Không đọc được dữ liệu giá từ Vietstock cho mã " + symbol);
            }

            String dateText = cols.get(0).text();
            String closeText = cols.get(1).text();

            LocalDate priceDate = parseDate(dateText);
            BigDecimal closePrice = parseDecimal(closeText);
            if (closePrice.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalStateException("Giá đóng cửa không hợp lệ cho mã " + symbol);
            }

            return new VietstockSnapshot(priceDate, closePrice);
        } catch (Exception e) {
            throw new IllegalStateException("Lỗi lấy giá Vietstock cho mã " + symbol, e);
        }
    }

    private LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return LocalDate.now();
        }
        List<DateTimeFormatter> patterns = List.of(
                DateTimeFormatter.ofPattern("dd/MM/uuuu"),
                DateTimeFormatter.ofPattern("d/M/uuuu"),
                DateTimeFormatter.ISO_LOCAL_DATE);
        for (DateTimeFormatter f : patterns) {
            try {
                return LocalDate.parse(raw.trim(), f);
            } catch (DateTimeParseException ignored) {
                // try next pattern
            }
        }
        return LocalDate.now();
    }

    private BigDecimal parseDecimal(String raw) {
        if (raw == null) {
            return BigDecimal.ZERO;
        }
        String normalized = raw.trim().replace(",", "");
        if (normalized.isBlank() || "-".equals(normalized)) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(normalized);
    }

    private record VietstockSnapshot(LocalDate priceDate, BigDecimal closePrice) {
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
