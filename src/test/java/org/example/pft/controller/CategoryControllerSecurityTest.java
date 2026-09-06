package org.example.pft.controller;

import org.example.pft.dto.category.CategoryByTypeData;
import org.example.pft.dto.category.CategoryData;
import org.example.pft.dto.category.CategoryRequest;
import org.example.pft.dto.category.CategoryResponse;
import org.example.pft.dto.category.CreateCategoryData;
import org.example.pft.enums.CategoryType;
import org.example.pft.exception.BusinessConflictException;
import org.example.pft.exception.BusinessValidationException;
import org.example.pft.exception.ResourceNotFoundException;
import org.example.pft.security.CustomUserDetailsService;
import org.example.pft.security.JwtAuthenticationFilter;
import org.example.pft.security.JwtService;
import org.example.pft.security.RestAccessDeniedHandler;
import org.example.pft.security.RestAuthenticationEntityPoint;
import org.example.pft.security.SecurityConfig;
import org.example.pft.service.CategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryController.class)
@AutoConfigureMockMvc
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RestAuthenticationEntityPoint.class,
        RestAccessDeniedHandler.class
})
class CategoryControllerSecurityTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CategoryService categoryService;

    @MockitoBean
    JwtService jwtService;

    @MockitoBean
    CustomUserDetailsService userDetailsService;

    private CategoryResponse<Map<CategoryType, List<CategoryData>>> getAllResponse;
    private CategoryResponse<List<CategoryByTypeData>> getByTypeResponse;
    private CategoryResponse<CreateCategoryData> createResponse;

    @BeforeEach
    void setup() {
        getAllResponse = new CategoryResponse<>();
        getAllResponse.setSuccess(true);
        getAllResponse.setMessage("Category list fetched successfully");
        getAllResponse.setData(Map.of(
                CategoryType.INCOME,
                List.of(new CategoryData(1L, "Salary", "money-icon")),
                CategoryType.EXPENSE,
                List.of(new CategoryData(2L, "Food", "food-icon"))
        ));

        getByTypeResponse = new CategoryResponse<>();
        getByTypeResponse.setSuccess(true);
        getByTypeResponse.setMessage("Category list fetched successfully");
        getByTypeResponse.setData(List.of(
                new CategoryByTypeData(2L, "Food", "food-icon", "food.png")
        ));

        createResponse = new CategoryResponse<>();
        createResponse.setSuccess(true);
        createResponse.setMessage("Category created successfully");
        createResponse.setData(
                new CreateCategoryData(2L, "Food", "EXPENSE", "food-icon", "food.png")
        );
    }

    @Test
    void getAll_withoutToken_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/api/categories"));

        verify(categoryService, never()).getAll();
    }

    @Test
    @WithMockUser
    void getAll_withAuthenticatedUser_shouldReturn200() throws Exception {
        when(categoryService.getAll())
                .thenReturn(getAllResponse);

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.INCOME[0].id").value(1))

                .andExpect(jsonPath("$.data.EXPENSE[0].id").value(2));


        verify(categoryService).getAll();
    }

    @Test
    void getByType_withoutToken_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/categories")
                        .param("type", "expense"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/api/categories"));

        verify(categoryService, never()).getByType(any());
    }

    @Test
    @WithMockUser
    void getByType_withAuthenticatedUser_shouldReturn200() throws Exception {
        when(categoryService.getByType("expense"))
                .thenReturn(getByTypeResponse);

        mockMvc.perform(get("/api/categories")
                        .param("type", "expense"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Category list fetched successfully"))
                .andExpect(jsonPath("$.data[0].id").value(2));

        verify(categoryService).getByType("expense");
    }

    @Test
    @WithMockUser
    void getByType_withInvalidType_shouldReturn422() throws Exception {
        when(categoryService.getByType("saving"))
                .thenThrow(new BusinessValidationException("Invalid category type: saving"));

        mockMvc.perform(get("/api/categories")
                        .param("type", "saving"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid category type: saving"));

        verify(categoryService).getByType("saving");
    }

    @Test
    void create_withoutToken_shouldReturn401() throws Exception {
        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {
                    "name": "Food",
                    "type": "expense",
                    "emoji": "food-icon"
                }
                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/api/categories"));

        verify(categoryService, never()).create(any(CategoryRequest.class));
    }

    @Test
    @WithMockUser
    void create_withAuthenticatedUser_shouldReturn201() throws Exception {
        when(categoryService.create(any(CategoryRequest.class)))
                .thenReturn(createResponse);

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {
                    "name": "Food",
                    "type": "expense",
                    "emoji": "food-icon"
                }
                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(2));

        verify(categoryService).create(any(CategoryRequest.class));
    }

    @Test
    @WithMockUser
    void create_withBlankName_shouldReturn422() throws Exception {
        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {
                    "name": "",
                    "type": "expense",
                    "emoji": "food-icon"
                }
                """))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"));

        verify(categoryService, never()).create(any(CategoryRequest.class));
    }

    @Test
    @WithMockUser
    void create_withoutType_shouldReturn422() throws Exception {
        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {
                    "name": "Food",
                    "emoji": "food-icon"
                }
                """))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"));

        verify(categoryService, never()).create(any(CategoryRequest.class));
    }

    @Test
    @WithMockUser
    void create_withExistingCategoryName_shouldReturn409() throws Exception {
        when(categoryService.create(any(CategoryRequest.class)))
                .thenThrow(new BusinessConflictException("Category name already exists"));

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {
                    "name": "Food",
                    "type": "expense",
                    "emoji": "food-icon"
                }
                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Category name already exists"));

        verify(categoryService).create(any(CategoryRequest.class));
    }

    @Test
    @WithMockUser
    void create_whenIconNotFound_shouldReturn404() throws Exception {
        when(categoryService.create(any(CategoryRequest.class)))
                .thenThrow(new ResourceNotFoundException("Icon not found"));

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {
                    "name": "Food",
                    "type": "expense",
                    "emoji": "food-icon"
                }
                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Icon not found"));

        verify(categoryService).create(any(CategoryRequest.class));
    }
}
