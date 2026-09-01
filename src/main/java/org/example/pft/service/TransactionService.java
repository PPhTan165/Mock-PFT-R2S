package org.example.pft.service;

import org.example.pft.dto.transaction.*;

import java.util.List;

public interface TransactionService {
    TransactionResponse<CreateTransactionData> create(TransactionRequest request);
    TransactionResponse<List<HistoryData>> showHistory(HistoryRequest request);
}
