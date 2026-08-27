package org.example.pft.service;

import org.example.pft.dto.transaction.CreateTransactionData;
import org.example.pft.dto.transaction.TransactionRequest;
import org.example.pft.dto.transaction.TransactionResponse;

public interface TransactionService {
    TransactionResponse<CreateTransactionData> create(TransactionRequest request);
}
