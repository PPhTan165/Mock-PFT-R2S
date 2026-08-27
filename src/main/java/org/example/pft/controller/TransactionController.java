package org.example.pft.controller;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.example.pft.dto.transaction.CreateTransactionData;
import org.example.pft.dto.transaction.TransactionRequest;
import org.example.pft.dto.transaction.TransactionResponse;
import org.example.pft.service.TransactionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/transactions")
@AllArgsConstructor
public class TransactionController {
    private final TransactionService transactionService;

    @PostMapping
    public ResponseEntity<TransactionResponse<CreateTransactionData>> create(
            @Valid @RequestBody TransactionRequest request
            ){
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.create(request));
    }
}
