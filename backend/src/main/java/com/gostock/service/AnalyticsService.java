package com.gostock.service;

import com.gostock.dto.*;
import com.gostock.entity.*;
import com.gostock.entity.enums.CashFlowType;
import com.gostock.repository.*;
import com.gostock.service.contract.AnalyticsServiceContract;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AnalyticsService implements AnalyticsServiceContract {
        private final PositionRepository positionRepo;
        private final AccountRepository accountRepo;
        private final CashFlowRepository cashFlowRepo;
        private final PriceHistoryRepository priceHistoryRepo;
        private final TransactionRepository transactionRepo;

        // ── Portfolio Summary (Holdings + Performance + Allocation) ──────────

        @Override
        @Transactional(readOnly = true)
        public PortfolioSummaryResponse getSummary(Long accountId) {
                accountRepo.findById(accountId)
                                .orElseThrow(() -> new EntityNotFoundException("Account not found: " + accountId));

                List<Position> positions = positionRepo.findActiveByAccountId(accountId);

                BigDecimal totalInvested = BigDecimal.ZERO;
                BigDecimal currentValue = BigDecimal.ZERO;

                List<HoldingResponse> holdings = positions.stream().map(pos -> {
                        BigDecimal marketValue = pos.getCurrentPrice()
                                        .multiply(BigDecimal.valueOf(pos.getHoldingVolume()));
                        BigDecimal invested = pos.getAvgCost()
                                        .multiply(BigDecimal.valueOf(pos.getHoldingVolume()));
                        BigDecimal pnl = marketValue.subtract(invested);
                        BigDecimal pnlPct = invested.compareTo(BigDecimal.ZERO) > 0
                                        ? pnl.divide(invested, 4, RoundingMode.HALF_UP)
                                                        .multiply(BigDecimal.valueOf(100))
                                        : BigDecimal.ZERO;

                        return HoldingResponse.builder()
                                        .tickerSymbol(pos.getTicker().getSymbol())
                                        .tickerName(pos.getTicker().getName())
                                        .exchange(pos.getTicker().getExchange() != null
                                                        ? pos.getTicker().getExchange().name()
                                                        : "")
                                        .holdingVolume(pos.getHoldingVolume())
                                        .avgCost(pos.getAvgCost())
                                        .currentPrice(pos.getCurrentPrice())
                                        .marketValue(marketValue)
                                        .unrealizedPnL(pnl)
                                        .unrealizedPnLPct(pnlPct)
                                        .build();
                }).collect(Collectors.toList());

                for (HoldingResponse h : holdings) {
                        totalInvested = totalInvested
                                        .add(h.getAvgCost().multiply(BigDecimal.valueOf(h.getHoldingVolume())));
                        currentValue = currentValue.add(h.getMarketValue());
                }

                BigDecimal unrealizedPnL = currentValue.subtract(totalInvested);

                // Realized PnL = tổng returnAmount của các lệnh bán đã hoàn thành
                BigDecimal realizedPnL = transactionRepo
                                .search(null, com.gostock.entity.enums.TradeType.SELL, null, null, accountId)
                                .stream()
                                .map(t -> t.getReturnAmount() != null ? t.getReturnAmount() : BigDecimal.ZERO)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal totalPnL = unrealizedPnL.add(realizedPnL);
                BigDecimal returnRate = totalInvested.compareTo(BigDecimal.ZERO) > 0
                                ? totalPnL.divide(totalInvested, 4, RoundingMode.HALF_UP)
                                                .multiply(BigDecimal.valueOf(100))
                                : BigDecimal.ZERO;

                // Phân bổ danh mục (allocation)
                List<AllocationItem> allocation = buildAllocation(holdings, currentValue);

                return PortfolioSummaryResponse.builder()
                                .totalInvested(totalInvested)
                                .currentValue(currentValue)
                                .unrealizedPnL(unrealizedPnL)
                                .realizedPnL(realizedPnL)
                                .totalPnL(totalPnL)
                                .returnRate(returnRate)
                                .holdings(holdings)
                                .allocation(allocation)
                                .build();
        }

        private List<AllocationItem> buildAllocation(List<HoldingResponse> holdings, BigDecimal totalValue) {
                if (totalValue.compareTo(BigDecimal.ZERO) == 0)
                        return List.of();
                return holdings.stream().map(h -> {
                        BigDecimal pct = h.getMarketValue()
                                        .divide(totalValue, 4, RoundingMode.HALF_UP)
                                        .multiply(BigDecimal.valueOf(100));
                        return AllocationItem.builder()
                                        .label(h.getTickerSymbol())
                                        .value(h.getMarketValue())
                                        .percentage(pct)
                                        .build();
                }).toList();
        }

        // ── Cash Summary ─────────────────────────────────────────────────────

        @Override
        @Transactional(readOnly = true)
        public CashSummaryResponse getCashSummary(Long accountId) {
                Account account = accountRepo.findById(accountId)
                                .orElseThrow(() -> new EntityNotFoundException("Account not found: " + accountId));

                BigDecimal totalDeposit = cashFlowRepo.sumByAccountAndType(accountId, CashFlowType.DEPOSIT);
                BigDecimal totalWithdraw = cashFlowRepo.sumByAccountAndType(accountId, CashFlowType.WITHDRAW);

                return CashSummaryResponse.builder()
                                .accountId(accountId)
                                .accountNumber(account.getAccountNumber())
                                .brokerCode(account.getBroker().getCode())
                                .totalDeposit(totalDeposit)
                                .totalWithdraw(totalWithdraw)
                                .netCashIn(totalDeposit.subtract(totalWithdraw))
                                .cashBalance(account.getCashBalance())
                                .purchasingPower(account.getPurchasingPower())
                                .availableForWithdrawal(account.getAvailableForWithdrawal())
                                .build();
        }
}
