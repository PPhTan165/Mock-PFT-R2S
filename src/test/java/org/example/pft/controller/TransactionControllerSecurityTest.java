package org.example.pft.controller;

import org.example.pft.dto.transaction.CreateTransactionData;
import org.example.pft.dto.transaction.HistoryData;
import org.example.pft.dto.transaction.HistoryRequest;
import org.example.pft.dto.transaction.TransactionCategoryData;
import org.example.pft.dto.transaction.TransactionRequest;
import org.example.pft.dto.transaction.TransactionResponse;
import org.example.pft.enums.CategoryType;
import org.example.pft.exception.ResourceNotFoundException;
import org.example.pft.security.CustomUserDetailsService;
import org.example.pft.security.JwtAuthenticationFilter;
import org.example.pft.security.JwtService;
import org.example.pft.security.RestAccessDeniedHandler;
import org.example.pft.security.RestAuthenticationEntityPoint;
import org.example.pft.security.SecurityConfig;
import org.example.pft.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
@AutoConfigureMockMvc
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RestAuthenticationEntityPoint.class,
        RestAccessDeniedHandler.class
})
class TransactionControllerSecurityTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    TransactionService transactionService;

    @MockitoBean
    JwtService jwtService;

    @MockitoBean
    CustomUserDetailsService userDetailsService;

    private TransactionResponse<CreateTransactionData> createResponse;
    private TransactionResponse<List<HistoryData>> historyResponse;

    @BeforeEach
    void setup() {
        createResponse = new TransactionResponse<>();
        createResponse.setSuccess(true);
        createResponse.setMessage("Transaction added successfully");
        createResponse.setData(new CreateTransactionData(
                100L,
                new BigDecimal("125000.50"),
                "Lunch",
                new TransactionCategoryData(
                        20L,
                        "Food",
                        "EXPENSE",
                        "food-icon",
                        "food.png"
                ),
                LocalDate.of(2026, 9, 7)
        ));

        historyResponse = new TransactionResponse<>();
        historyResponse.setSuccess(true);
        historyResponse.setMessage("Transaction history fetched successfully");
        historyResponse.setData(List.of(
                new HistoryData(
                        20L,
                        "Food",
                        "food-icon",
                        CategoryType.EXPENSE,
                        new BigDecimal("125000.50"),
                        LocalDate.of(2026, 9, 7)
                )
        ));
    }

    @Test
    void create_withoutToken_shouldReturn401() throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {
                    "amount": 125000.50,
                    "note": "Lunch",
                    "categoryId": 20,
                    "date": "2026-09-07"
                }
                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/api/transactions"));

        verify(transactionService, never()).create(any(TransactionRequest.class));
    }

    @Test
    @WithMockUser
    void create_withAuthenticatedUser_shouldReturn201() throws Exception {
        when(transactionService.create(any(TransactionRequest.class)))
                .thenReturn(createResponse);

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {
                    "amount": 125000.50,
                    "note": "Lunch",
                    "categoryId": 20,
                    "date": "2026-09-07"
                }
                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Transaction added successfully"))
                .andExpect(jsonPath("$.data.id").value(100));

        ArgumentCaptor<TransactionRequest> requestCaptor = ArgumentCaptor.forClass(TransactionRequest.class);
        verify(transactionService).create(requestCaptor.capture());
        assertEquals(new BigDecimal("125000.50"), requestCaptor.getValue().getAmount());
        assertEquals("Lunch", requestCaptor.getValue().getNote());
        assertEquals(20L, requestCaptor.getValue().getCategoryId());
        assertEquals(LocalDate.of(2026, 9, 7), requestCaptor.getValue().getDate());
    }

    @Test
    @WithMockUser
    void create_withoutAmount_shouldReturn422() throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {
                    "note": "Lunch",
                    "categoryId": 20,
                    "date": "2026-09-07"
                }
                """))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"));

        verify(transactionService, never()).create(any(TransactionRequest.class));
    }

    @Test
    @WithMockUser
    void create_withInvalidAmount_shouldReturn422() throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {
                    "amount": 0,
                    "note": "Lunch",
                    "categoryId": 20,
                    "date": "2026-09-07"
                }
                """))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"));

        verify(transactionService, never()).create(any(TransactionRequest.class));
    }

    @Test
    @WithMockUser
    void create_whenCategoryNotFound_shouldReturn404() throws Exception {
        when(transactionService.create(any(TransactionRequest.class)))
                .thenThrow(new ResourceNotFoundException("Category not found"));

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {
                    "amount": 125000.50,
                    "note": "Lunch",
                    "categoryId": 99,
                    "date": "2026-09-07"
                }
                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Category not found"));

        verify(transactionService).create(any(TransactionRequest.class));
    }

    @Test
    void showHistory_withoutToken_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/transactions/history")
                        .param("startDate", "2026-09-01")
                        .param("endDate", "2026-09-30")
                        .param("type", "EXPENSE"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/api/transactions/history"));

        verify(transactionService, never()).showHistory(any(HistoryRequest.class));
    }

    @Test
    @WithMockUser
    void showHistory_withAuthenticatedUser_shouldReturn200() throws Exception {
        when(transactionService.showHistory(any(HistoryRequest.class)))
                .thenReturn(historyResponse);

        mockMvc.perform(get("/api/transactions/history")
                        .param("startDate", "2026-09-01")
                        .param("endDate", "2026-09-30")
                        .param("categoryId", "20")
                        .param("type", "EXPENSE")
                        .param("page", "2")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Transaction history fetched successfully"))
                .andExpect(jsonPath("$.data[0].id").value(20));

        ArgumentCaptor<HistoryRequest> requestCaptor = ArgumentCaptor.forClass(HistoryRequest.class);
        verify(transactionService).showHistory(requestCaptor.capture());
        assertEquals(20L, requestCaptor.getValue().getCategoryId());
    }

    @Test
    @WithMockUser
    void showHistory_withoutType_shouldReturn422() throws Exception {
        mockMvc.perform(get("/api/transactions/history")
                        .param("startDate", "2026-09-01")
                        .param("endDate", "2026-09-30"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"));

        verify(transactionService, never()).showHistory(any(HistoryRequest.class));
    }

    @Test
    @WithMockUser
    void showHistory_withInvalidDateRange_shouldReturn422() throws Exception {
        mockMvc.perform(get("/api/transactions/history")
                        .param("startDate", "2026-09-30")
                        .param("endDate", "2026-09-01")
                        .param("type", "EXPENSE"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"));

        verify(transactionService, never()).showHistory(any(HistoryRequest.class));
    }

    @Test
    @WithMockUser
    void showHistory_whenCategoryNotFound_shouldReturn404() throws Exception {
        when(transactionService.showHistory(any(HistoryRequest.class)))
                .thenThrow(new ResourceNotFoundException("Category not found"));

        mockMvc.perform(get("/api/transactions/history")
                        .param("startDate", "2026-09-01")
                        .param("endDate", "2026-09-30")
                        .param("categoryId", "99")
                        .param("type", "EXPENSE"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Category not found"));

        verify(transactionService).showHistory(any(HistoryRequest.class));
    }
}
