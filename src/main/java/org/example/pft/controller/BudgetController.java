package org.example.pft.controller;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.example.pft.dto.budget.BudgetData;
import org.example.pft.dto.budget.BudgetResponse;
import org.example.pft.dto.budget.CreateBudgetRequest;
import org.example.pft.dto.budget.GetAllBudgetRequest;
import org.example.pft.dto.budget.UpdateBudgetRequest;
import org.example.pft.service.BudgetService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/budgets")
@AllArgsConstructor
public class BudgetController {
    private final BudgetService budgetService;

    @PostMapping
    public ResponseEntity<BudgetResponse<BudgetData>> create (
            @RequestBody @Valid CreateBudgetRequest request){
        return ResponseEntity.status(HttpStatus.CREATED).body(budgetService.create(request));
    }

    @GetMapping
    public ResponseEntity<BudgetResponse<List<BudgetData>>> getAll(@ModelAttribute @Valid GetAllBudgetRequest request) {
        return ResponseEntity.ok().body(budgetService.getAll(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BudgetResponse<BudgetData>> update(
            @PathVariable Long id,
            @RequestBody @Valid UpdateBudgetRequest request
    ) {
        return ResponseEntity.ok().body(budgetService.updateAmount(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        budgetService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
