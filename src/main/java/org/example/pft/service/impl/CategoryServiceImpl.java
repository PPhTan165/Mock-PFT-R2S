package org.example.pft.service.impl;

import lombok.AllArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.example.pft.dto.category.CategoryByTypeData;
import org.example.pft.dto.category.CategoryData;
import org.example.pft.dto.category.CategoryResponse;
import org.example.pft.entity.Category;
import org.example.pft.entity.CategoryIcon;
import org.example.pft.entity.User;
import org.example.pft.enums.CategoryType;
import org.example.pft.exception.BusinessValidationException;
import org.example.pft.helper.CurrentUserHelper;
import org.example.pft.repository.CategoryRepository;
import org.example.pft.repository.UserRepository;
import org.example.pft.service.CategoryService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final CurrentUserHelper currentUser;

    private CategoryData mapToCategoryData(Category category) {
        CategoryIcon icon = category.getCategoryIcon();

        return new CategoryData(
                category.getId(),
                icon.getCategoryName(),
                icon.getEmoji()
        );
    }

    private CategoryByTypeData mapToCategoryByTypeData(Category category) {
        CategoryIcon icon = category.getCategoryIcon();

        return new CategoryByTypeData(
                category.getId(),
                icon.getCategoryName(),
                icon.getEmoji(),
                icon.getIconUrl()
        );
    }

    private <T> CategoryResponse<T> successResponse(T data, String message) {
        CategoryResponse<T> response = new CategoryResponse<>();
        response.setSuccess(true);
        response.setMessage(message);
        response.setData(data);

        return response;
    }


    @Override
    public CategoryResponse<Map<CategoryType, List<CategoryData>>> getAll() {
        Map<CategoryType, List<CategoryData>> data = new EnumMap<>(CategoryType.class);

        for (CategoryType type : CategoryType.values()) {
            data.put(type, new ArrayList<>());
        }

        User user = getCurrentUser();

        categoryRepository.findAllByUser(user).forEach(category ->
                data.get(category.getType()).add(mapToCategoryData(category))
        );

        return successResponse(data, "Category list fetched successfully");
    }

    @Override
    public CategoryResponse<List<CategoryByTypeData>> getByType(String type) {
        CategoryType categoryType = parseCategoryType(type); // ep kieu String sang CategoryType
        User user = getCurrentUser();

        List<CategoryByTypeData> data = categoryRepository.findAllByTypeAndUser(categoryType,user)
                .stream()
                .map(this::mapToCategoryByTypeData)
                .collect(Collectors.toList());

        // Check size
        if (data.size() == 0) {
            return successResponse(data, "No categories found for the given type");
        } else {
            return successResponse(data, "Category list fetched successfully");
        }

    }

    //Chuyen doi kieu String sang CategoryType
    private CategoryType parseCategoryType(String type) {
        try {
            return CategoryType.valueOf(type.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessValidationException("Invalid category type: " + type);
        }
    }

    private User getCurrentUser() {
        return currentUser.getCurrentUser();
    }
}
