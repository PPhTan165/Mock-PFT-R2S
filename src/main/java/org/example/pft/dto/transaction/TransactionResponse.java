package org.example.pft.dto.transaction;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

@Data
@JsonPropertyOrder({"success","message","data"})
public class TransactionResponse<T> {
    private boolean success;
    private String message;
    private T data;
}
