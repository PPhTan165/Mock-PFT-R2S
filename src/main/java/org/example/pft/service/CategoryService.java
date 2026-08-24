package org.example.pft.service;


import org.example.pft.dto.category.CategoryByTypeData;
import org.example.pft.dto.category.CategoryData;
import org.example.pft.dto.category.CategoryResponse;
import org.example.pft.enums.CategoryType;

import java.util.List;
import java.util.Map;

public interface CategoryService {
    CategoryResponse<Map<CategoryType, List<CategoryData>>> getAll();
    CategoryResponse<List<CategoryByTypeData>> getByType(String type);
}
