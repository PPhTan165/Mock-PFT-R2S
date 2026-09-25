package org.example.pft.service.impl;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.example.pft.dto.transaction.*;
import org.example.pft.entity.Category;
import org.example.pft.entity.CategoryIcon;
import org.example.pft.entity.Transaction;
import org.example.pft.entity.User;
import org.example.pft.exception.ResourceNotFoundException;
import org.example.pft.helper.CurrentUserHelper;
import org.example.pft.repository.CategoryRepository;
import org.example.pft.repository.TransactionRepository;
import org.example.pft.service.TransactionService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

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
                        category.getType().name(),
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
    @Transactional
    public TransactionResponse<CreateTransactionData> create(TransactionRequest request){
        User currentUser = currentUserHelper.getCurrentUser();

        Category category = categoryRepository.findByIdAndUser(request.getCategoryId(),currentUser)
                .orElseThrow(()-> new ResourceNotFoundException("Category not found"));

        Transaction data = new Transaction();
        data.setAmount(request.getAmount());
        data.setNote(request.getNote());
        data.setDate(request.getDate());
        data.setCategory(category);
        data.setUser(currentUser);

        Transaction saved = transactionRepository.save(data);
        CreateTransactionData resData = mapToCreateTransaction(saved);

        return successResponse(resData,"Transaction added successfully");
    }

    @Override
    public TransactionResponse<List<HistoryData>> showHistory(HistoryRequest request){
        User user = currentUserHelper.getCurrentUser();
        Long userId = user.getId();

        Long categoryId = request.getCategoryId();
        if (categoryId != null) {
            categoryRepository.findById(categoryId)
                    .orElseThrow(()-> new ResourceNotFoundException("Category not found"));
        }

        Pageable pageable = PageRequest.of(
                resolvePage(request.getPage()) - 1,
                resolveSize(request.getSize())
        );

        List<HistoryData> historyDataList = transactionRepository
                .showHistory(
                        userId,
                        request.getStartDate(),
                        request.getEndDate(),
                        categoryId,
                        request.getType(),
                        pageable);

        return successResponse(historyDataList,"Transaction history fetched successfully");
    }

    private int resolvePage(Integer page) {
        if (page == null || page < HistoryRequest.DEFAULT_PAGE) {
            return HistoryRequest.DEFAULT_PAGE;
        }

        return page;
    }

    private int resolveSize(Integer size) {
        if (size == null || size < 1) {
            return HistoryRequest.DEFAULT_SIZE;
        }

        return Math.min(size, HistoryRequest.MAX_SIZE);
    }
}
