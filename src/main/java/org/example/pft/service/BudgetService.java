package org.example.pft.service;

import org.example.pft.dto.budget.*;

import java.util.List;

public interface BudgetService {
    BudgetResponse<BudgetData> create(CreateBudgetRequest request);
    BudgetResponse<BudgetData> updateAmount(Long id, UpdateBudgetRequest request);
    void delete(Long id);
    BudgetResponse<List<BudgetData>> getAll(GetAllBudgetRequest request);
}
