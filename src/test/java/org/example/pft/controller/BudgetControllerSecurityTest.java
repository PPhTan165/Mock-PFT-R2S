package org.example.pft.controller;

import org.example.pft.dto.budget.BudgetCategory;
import org.example.pft.dto.budget.BudgetData;
import org.example.pft.dto.budget.BudgetResponse;
import org.example.pft.dto.budget.CreateBudgetRequest;
import org.example.pft.dto.budget.GetAllBudgetRequest;
import org.example.pft.dto.budget.UpdateBudgetRequest;
import org.example.pft.enums.CategoryType;
import org.example.pft.exception.BusinessValidationException;
import org.example.pft.exception.ResourceNotFoundException;
import org.example.pft.security.CustomUserDetailsService;
import org.example.pft.security.JwtAuthenticationFilter;
import org.example.pft.security.JwtService;
import org.example.pft.security.RestAccessDeniedHandler;
import org.example.pft.security.RestAuthenticationEntityPoint;
import org.example.pft.security.SecurityConfig;
import org.example.pft.service.BudgetService;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BudgetController.class)
@AutoConfigureMockMvc
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RestAuthenticationEntityPoint.class,
        RestAccessDeniedHandler.class
})
class BudgetControllerSecurityTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    BudgetService budgetService;

    @MockitoBean
    JwtService jwtService;

    @MockitoBean
    CustomUserDetailsService userDetailsService;

    private BudgetResponse<BudgetData> budgetResponse;
    private BudgetResponse<List<BudgetData>> budgetListResponse;

    @BeforeEach
    void setup() {
        BudgetData budgetData = new BudgetData(
                200L,
                new BudgetCategory(20L, "Food", "food-icon", "food.png"),
                new BigDecimal("1000000"),
                9,
                2026,
                CategoryType.EXPENSE
        );

        budgetResponse = new BudgetResponse<>();
        budgetResponse.setSuccess(true);
        budgetResponse.setMessage("Budget saved successfully");
        budgetResponse.setData(budgetData);

        budgetListResponse = new BudgetResponse<>();
        budgetListResponse.setSuccess(true);
        budgetListResponse.setMessage("Budget list fetched successfully");
        budgetListResponse.setData(List.of(budgetData));
    }

    @Test
    void create_withoutToken_shouldReturn401() throws Exception {
        mockMvc.perform(post("/api/budgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {
                    "categoryId": 20,
                    "amount": 1000000,
                    "month": 9,
                    "year": 2026
                }
                """))
                .andExpect(status().isUnauthorized());

        verify(budgetService, never()).create(any(CreateBudgetRequest.class));
    }

    @Test
    @WithMockUser
    void create_withAuthenticatedUser_shouldReturn201() throws Exception {
        when(budgetService.create(any(CreateBudgetRequest.class)))
                .thenReturn(budgetResponse);

        mockMvc.perform(post("/api/budgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {
                    "categoryId": 20,
                    "amount": 1000000,
                    "month": 9,
                    "year": 2026
                }
                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(200))
                .andExpect(jsonPath("$.data.category.id").value(20));

        ArgumentCaptor<CreateBudgetRequest> requestCaptor = ArgumentCaptor.forClass(CreateBudgetRequest.class);
        verify(budgetService).create(requestCaptor.capture());
        assertEquals(20L, requestCaptor.getValue().getCategoryId());
    }

    @Test
    @WithMockUser
    void create_withoutCategoryId_shouldReturn422() throws Exception {
        mockMvc.perform(post("/api/budgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {
                    "amount": 1000000,
                    "month": 9,
                    "year": 2026
                }
                """))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"));

        verify(budgetService, never()).create(any(CreateBudgetRequest.class));
    }

    @Test
    @WithMockUser
    void create_withIncomeCategory_shouldReturn422() throws Exception {
        when(budgetService.create(any(CreateBudgetRequest.class)))
                .thenThrow(new BusinessValidationException("Category type must be EXPENSE"));

        mockMvc.perform(post("/api/budgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {
                    "categoryId": 30,
                    "amount": 1000000,
                    "month": 9,
                    "year": 2026
                }
                """))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.success").value(false));

        verify(budgetService).create(any(CreateBudgetRequest.class));
    }

    @Test
    @WithMockUser
    void getAll_withAuthenticatedUser_shouldReturn200() throws Exception {
        when(budgetService.getAll(any(GetAllBudgetRequest.class)))
                .thenReturn(budgetListResponse);

        mockMvc.perform(get("/api/budgets")
                        .param("month", "9")
                        .param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(200))
                .andExpect(jsonPath("$.data[0].category.id").value(20));

        verify(budgetService).getAll(any(GetAllBudgetRequest.class));
    }

    @Test
    @WithMockUser
    void getAll_withInvalidMonth_shouldReturn422() throws Exception {
        mockMvc.perform(get("/api/budgets")
                        .param("month", "13")
                        .param("year", "2026"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"));

        verify(budgetService, never()).getAll(any(GetAllBudgetRequest.class));
    }

    @Test
    @WithMockUser
    void update_withAuthenticatedUser_shouldReturn200() throws Exception {
        when(budgetService.updateAmount(eq(200L), any(UpdateBudgetRequest.class)))
                .thenReturn(budgetResponse);

        mockMvc.perform(put("/api/budgets/{id}", 200L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {
                    "amount": 1500000
                }
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(200))
                .andExpect(jsonPath("$.data.category.id").value(20));

        verify(budgetService).updateAmount(eq(200L), any(UpdateBudgetRequest.class));
    }

    @Test
    @WithMockUser
    void update_whenBudgetNotFound_shouldReturn404() throws Exception {
        when(budgetService.updateAmount(eq(99L), any(UpdateBudgetRequest.class)))
                .thenThrow(new ResourceNotFoundException("Budget not found"));

        mockMvc.perform(put("/api/budgets/{id}", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {
                    "amount": 1500000
                }
                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));

        verify(budgetService).updateAmount(eq(99L), any(UpdateBudgetRequest.class));
    }

    @Test
    @WithMockUser
    void update_withInvalidAmount_shouldReturn422() throws Exception {
        mockMvc.perform(put("/api/budgets/{id}", 200L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {
                    "amount": 0
                }
                """))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"));

        verify(budgetService, never()).updateAmount(any(), any(UpdateBudgetRequest.class));
    }

    @Test
    @WithMockUser
    void delete_withAuthenticatedUser_shouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/budgets/{id}", 200L))
                .andExpect(status().isNoContent());

        verify(budgetService).delete(200L);
    }

    @Test
    @WithMockUser
    void delete_whenBudgetNotFound_shouldReturn404() throws Exception {
        org.mockito.Mockito.doThrow(new ResourceNotFoundException("Budget not found"))
                .when(budgetService)
                .delete(99L);

        mockMvc.perform(delete("/api/budgets/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));

        verify(budgetService).delete(99L);
    }
}
