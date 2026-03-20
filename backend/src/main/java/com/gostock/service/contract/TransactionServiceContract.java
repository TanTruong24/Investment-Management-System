package com.gostock.service.contract;

import com.gostock.dto.TransactionRequest;
import com.gostock.dto.TransactionResponse;
import com.gostock.dto.response.PagingResponse;
import com.gostock.entity.enums.TradeType;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

public interface TransactionServiceContract {
    TransactionResponse create(TransactionRequest req);

    TransactionResponse update(Long id, TransactionRequest req);

    PagingResponse<List<TransactionResponse>> search(String tickerSymbol,
                                                     TradeType trade,
                                                     LocalDate fromDate,
                                                     LocalDate toDate,
                                                     Long accountId,
                                                     Pageable pageable);

    List<TransactionResponse> importFromExcel(MultipartFile file, Long accountId) throws IOException;

    List<TransactionResponse> importStockTransactionHistoryExcel(MultipartFile file, Long accountId) throws IOException;

    List<TransactionResponse> importFundStatementExcel(MultipartFile file, Long accountId) throws IOException;

    List<TransactionResponse> importStockStatementExcel(MultipartFile file, Long accountId) throws IOException;
}
