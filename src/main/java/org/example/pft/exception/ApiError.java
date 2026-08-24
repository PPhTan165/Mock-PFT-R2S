package org.example.pft.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@AllArgsConstructor
public class ApiError {
    private boolean success;
    private String message;
    private List<ErrorData> errors;

}
