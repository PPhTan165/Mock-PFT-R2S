package org.example.pft.service.impl;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.example.pft.dto.budget.*;
import org.example.pft.entity.Budget;
import org.example.pft.entity.Category;
import org.example.pft.entity.CategoryIcon;
import org.example.pft.entity.User;
import org.example.pft.enums.CategoryType;
import org.example.pft.exception.BusinessValidationException;
import org.example.pft.exception.ResourceNotFoundException;
import org.example.pft.helper.CurrentUserHelper;
import org.example.pft.repository.BudgetRepository;
import org.example.pft.repository.CategoryIconRepository;
import org.example.pft.repository.CategoryRepository;
import org.example.pft.service.BudgetService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class BudgetServiceImpl implements BudgetService {
    private final CategoryRepository categoryRepository;
    private final CategoryIconRepository categoryIconRepository;
    private final CurrentUserHelper currentUserHelper;
    private final BudgetRepository budgetRepository;

    @Override
    @Transactional
    public BudgetResponse<BudgetData> create(CreateBudgetRequest request){
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(()-> new ResourceNotFoundException("Category not found with id: "
                        +request.getCategoryId()));

        if (category.getType() != CategoryType.EXPENSE) {
            throw new BusinessValidationException("Category type must be EXPENSE");
        }

        User user = currentUserHelper.getCurrentUser();
        Byte month = request.getMonth().byteValue();
        Short year = request.getYear().shortValue();

        Budget budget = budgetRepository.findByCategoryAndMonthAndYear(category, month, year)
                .orElseGet(() -> {
                    Budget newBudget = new Budget();
                    newBudget.setCategory(category);
                    newBudget.setUser(user);
                    newBudget.setMonth(month);
                    newBudget.setYear(year);
                    return newBudget;
                });

        budget.setAmount(request.getAmount());

        Budget saved = budgetRepository.save(budget);
        return successResponse(mapToData(saved),"Budget saved successfully");
    }

    @Override
    @Transactional
    public BudgetResponse<BudgetData> updateAmount(Long id, UpdateBudgetRequest request){
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(()-> new ResourceNotFoundException("Budget not found"));

        budget.setAmount(request.getAmount());
        budgetRepository.save(budget);
        return successResponse(mapToData(budget),"Update amount budget successfully");
    }

    @Override
    public void delete(Long id){
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(()-> new ResourceNotFoundException("Budget not found"));
        budgetRepository.delete(budget);
    }

    @Override
    public BudgetResponse<List<BudgetData>> getAll(GetAllBudgetRequest request){
        Byte month = request.getMonth().byteValue();
        Short year = request.getYear().shortValue();
        List<BudgetData> budgets = budgetRepository.findByMonthAndYear(month,year)
                .stream()
                .map(this::mapToData)
                .toList();
        return successResponse(budgets,"Budget list fetched successfully");
    }

    private BudgetData mapToData(Budget budget){
        Category category = categoryRepository.findById(budget.getCategory().getId())
                .orElseThrow(()-> new ResourceNotFoundException("Category not found with id: "
                        +budget.getCategory().getId()));

        BudgetCategory budgetCategory = getCategory(category);

        return new BudgetData(
                budget.getId(),
                budgetCategory,
                budget.getAmount(),
                budget.getMonth().intValue(),
                budget.getYear().intValue(),
                CategoryType.EXPENSE
        );

    }

    private BudgetCategory getCategory(Category category){
        Long categoryId = category.getId();
        Long iconId = category.getCategoryIcon().getId();

        CategoryIcon icon = categoryIconRepository.findById(iconId)
                .orElseThrow(()-> new ResourceNotFoundException("Category icon not found with id: "
                        +iconId));

        return new BudgetCategory(
                categoryId,
                icon.getCategoryName(),
                icon.getEmoji(),
                icon.getIconUrl()
        );

    }

    private <T> BudgetResponse<T> successResponse(T data, String message){
        BudgetResponse<T> response = new BudgetResponse<>();
        response.setSuccess(true);
        response.setMessage(message);
        response.setData(data);
        return response;
    }

}
