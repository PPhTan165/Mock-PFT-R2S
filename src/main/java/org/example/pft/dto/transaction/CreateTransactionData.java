package org.example.pft.dto.transaction;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.example.pft.entity.Category;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@JsonPropertyOrder({"id","amount","note","category","date"})
public class CreateTransactionData {
    private Long id;
    private BigDecimal amount;
    private String note;
    private TransactionCategoryData category;
    private LocalDate date;
}
