package org.example.pft.service;

import org.example.pft.dto.transaction.CreateTransactionData;
import org.example.pft.dto.transaction.HistoryData;
import org.example.pft.dto.transaction.HistoryRequest;
import org.example.pft.dto.transaction.TransactionRequest;
import org.example.pft.dto.transaction.TransactionResponse;
import org.example.pft.entity.Category;
import org.example.pft.entity.CategoryIcon;
import org.example.pft.entity.Transaction;
import org.example.pft.entity.User;
import org.example.pft.enums.CategoryType;
import org.example.pft.exception.ResourceNotFoundException;
import org.example.pft.helper.CurrentUserHelper;
import org.example.pft.repository.CategoryRepository;
import org.example.pft.repository.TransactionRepository;
import org.example.pft.service.impl.TransactionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @Mock
    TransactionRepository transactionRepository;

    @Mock
    CategoryRepository categoryRepository;

    @Mock
    CurrentUserHelper currentUserHelper;

    @InjectMocks
    TransactionServiceImpl transactionService;

    private User user;
    private Category foodCategory;
    private TransactionRequest transactionRequest;

    @BeforeEach
    void setup() {
        user = new User();
        user.setId(1L);
        user.setEmail("user@example.com");

        CategoryIcon foodIcon = new CategoryIcon();
        foodIcon.setId(2L);
        foodIcon.setCategoryName("Food");
        foodIcon.setEmoji("food-icon");
        foodIcon.setIconUrl("food.png");

        foodCategory = new Category();
        foodCategory.setId(20L);
        foodCategory.setUser(user);
        foodCategory.setCategoryIcon(foodIcon);
        foodCategory.setType(CategoryType.EXPENSE);

        transactionRequest = new TransactionRequest();
        transactionRequest.setAmount(new BigDecimal("125000.50"));
        transactionRequest.setNote("Lunch");
        transactionRequest.setCategoryId(20L);
        transactionRequest.setDate(LocalDate.of(2026, 9, 7));
    }

    @Test
    void create_withValidRequest_shouldSaveTransactionAndReturnResponse() {
        when(currentUserHelper.getCurrentUser())
                .thenReturn(user);
        when(categoryRepository.findById(20L))
                .thenReturn(Optional.of(foodCategory));
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> {
                    Transaction transaction = invocation.getArgument(0);
                    transaction.setId(100L);
                    return transaction;
                });

        TransactionResponse<CreateTransactionData> response = transactionService.create(transactionRequest);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Transaction added successfully", response.getMessage());
        assertEquals(100L, response.getData().getId());

        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(transactionCaptor.capture());

        Transaction savedTransaction = transactionCaptor.getValue();
        assertEquals(new BigDecimal("125000.50"), savedTransaction.getAmount());
        assertEquals("Lunch", savedTransaction.getNote());
        assertEquals(LocalDate.of(2026, 9, 7), savedTransaction.getDate());
        assertSame(user, savedTransaction.getUser());
        assertSame(foodCategory, savedTransaction.getCategory());

        verify(currentUserHelper).getCurrentUser();
        verify(categoryRepository).findById(20L);
    }

    @Test
    void create_whenCategoryNotFound_shouldThrowException() {
        when(currentUserHelper.getCurrentUser())
                .thenReturn(user);
        when(categoryRepository.findById(20L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> transactionService.create(transactionRequest)
        );

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void showHistory_withoutCategoryFilter_shouldReturnHistory() {
        HistoryRequest request = createHistoryRequest(null, 1, 10);
        List<HistoryData> history = List.of(
                new HistoryData(
                        20L,
                        "Food",
                        "food-icon",
                        CategoryType.EXPENSE,
                        new BigDecimal("125000.50"),
                        LocalDate.of(2026, 9, 7)
                )
        );

        when(currentUserHelper.getCurrentUser())
                .thenReturn(user);
        when(transactionRepository.showHistory(
                eq(1L),
                eq(LocalDate.of(2026, 9, 1)),
                eq(LocalDate.of(2026, 9, 30)),
                eq(null),
                eq(CategoryType.EXPENSE),
                any(Pageable.class)
        )).thenReturn(history);

        TransactionResponse<List<HistoryData>> response = transactionService.showHistory(request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Transaction history fetched successfully", response.getMessage());
        assertEquals(1, response.getData().size());
        assertEquals("Food", response.getData().get(0).getCategory().getName());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(transactionRepository).showHistory(
                eq(1L),
                eq(LocalDate.of(2026, 9, 1)),
                eq(LocalDate.of(2026, 9, 30)),
                eq(null),
                eq(CategoryType.EXPENSE),
                pageableCaptor.capture()
        );
        assertEquals(0, pageableCaptor.getValue().getPageNumber());
        assertEquals(10, pageableCaptor.getValue().getPageSize());

        verify(categoryRepository, never()).findById(any());
    }

    @Test
    void showHistory_withCategoryFilter_shouldValidateCategoryAndUsePaging() {
        HistoryRequest request = createHistoryRequest(20L, 2, 5);

        when(currentUserHelper.getCurrentUser())
                .thenReturn(user);
        when(categoryRepository.findById(20L))
                .thenReturn(Optional.of(foodCategory));
        when(transactionRepository.showHistory(
                eq(1L),
                eq(LocalDate.of(2026, 9, 1)),
                eq(LocalDate.of(2026, 9, 30)),
                eq(20L),
                eq(CategoryType.EXPENSE),
                any(Pageable.class)
        )).thenReturn(List.of());

        TransactionResponse<List<HistoryData>> response = transactionService.showHistory(request);

        assertTrue(response.isSuccess());
        assertTrue(response.getData().isEmpty());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(transactionRepository).showHistory(
                eq(1L),
                eq(LocalDate.of(2026, 9, 1)),
                eq(LocalDate.of(2026, 9, 30)),
                eq(20L),
                eq(CategoryType.EXPENSE),
                pageableCaptor.capture()
        );
        assertEquals(1, pageableCaptor.getValue().getPageNumber());
        assertEquals(5, pageableCaptor.getValue().getPageSize());

        verify(categoryRepository).findById(20L);
    }

    @Test
    void showHistory_whenCategoryFilterNotFound_shouldThrowException() {
        HistoryRequest request = createHistoryRequest(99L, 1, 10);

        when(currentUserHelper.getCurrentUser())
                .thenReturn(user);
        when(categoryRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> transactionService.showHistory(request)
        );

        verify(transactionRepository, never()).showHistory(
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
        );
    }

    private HistoryRequest createHistoryRequest(Long categoryId, Integer page, Integer size) {
        HistoryRequest request = new HistoryRequest();
        request.setStartDate(LocalDate.of(2026, 9, 1));
        request.setEndDate(LocalDate.of(2026, 9, 30));
        request.setCategoryId(categoryId);
        request.setType(CategoryType.EXPENSE);
        request.setPage(page);
        request.setSize(size);
        return request;
    }
}
