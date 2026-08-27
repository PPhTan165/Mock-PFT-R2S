package org.example.pft.service.impl;

import lombok.AllArgsConstructor;
import org.example.pft.dto.transaction.CreateTransactionData;
import org.example.pft.dto.transaction.TransactionCategoryData;
import org.example.pft.dto.transaction.TransactionRequest;
import org.example.pft.dto.transaction.TransactionResponse;
import org.example.pft.entity.Category;
import org.example.pft.entity.CategoryIcon;
import org.example.pft.entity.Transaction;
import org.example.pft.entity.User;
import org.example.pft.exception.ResourceNotFoundException;
import org.example.pft.helper.CurrentUserHelper;
import org.example.pft.repository.CategoryRepository;
import org.example.pft.repository.TransactionRepository;
import org.example.pft.service.TransactionService;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class TransactionServiceImpl implements TransactionService {
    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final CurrentUserHelper currentUserHelper;

    private CreateTransactionData mapToCreateTransaction(Transaction transaction){
        Category category = transaction.getCategory();
        CategoryIcon categoryIcon = category.getCategoryIcon();

        TransactionCategoryData categoryData =
                new TransactionCategoryData(
                        category.getId(),
                        categoryIcon.getCategoryName(),
                        category.getType(),
                        categoryIcon.getEmoji(),
                        categoryIcon.getIconUrl()
                );

        return new CreateTransactionData(
                transaction.getId(),
                transaction.getAmount(),
                transaction.getNote(),
                categoryData,
                transaction.getDate()
        );
    }

    private <T> TransactionResponse<T> successResponse(T data, String message){
        TransactionResponse<T> response = new TransactionResponse<>();
        response.setSuccess(true);
        response.setMessage(message);
        response.setData(data);

        return response;
    }


    @Override
    public TransactionResponse<CreateTransactionData> create(TransactionRequest request){
        User user = currentUserHelper.getCurrentUser();

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(()-> new ResourceNotFoundException("Category not found"));

        Transaction data = new Transaction();
        data.setAmount(request.getAmount());
        data.setNote(request.getNote());
        data.setDate(request.getDate());
        data.setCategory(category);
        data.setUser(user);

        Transaction saved = transactionRepository.save(data);
        CreateTransactionData resData = mapToCreateTransaction(saved);

        return successResponse(resData,"Transaction added successfully");
    }
}
