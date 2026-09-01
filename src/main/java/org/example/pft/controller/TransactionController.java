package org.example.pft.controller;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.example.pft.dto.transaction.*;
import org.example.pft.service.TransactionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping("/history")
    public ResponseEntity<TransactionResponse<List<HistoryData>>> showHistory(
            @ModelAttribute @Valid HistoryRequest request
            ){
        return ResponseEntity.ok().body(transactionService.showHistory(request));
    }
}
