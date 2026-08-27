package org.example.pft.dto.transaction;

import lombok.Data;

@Data
public class TransactionResponse<T> {
    private boolean success;
    private String message;
    private T data;
}
