package org.example.pft.exception;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ErrorData {
    private String field;
    private String message;
}
