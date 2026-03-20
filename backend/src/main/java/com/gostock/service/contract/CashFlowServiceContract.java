package com.gostock.service.contract;

import com.gostock.dto.CashFlowRequest;
import com.gostock.entity.CashFlow;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface CashFlowServiceContract {
    CashFlow create(CashFlowRequest req);

    List<CashFlow> listByAccount(Long accountId);

    List<CashFlow> importFromExcel(MultipartFile file, Long accountId) throws IOException;
}
