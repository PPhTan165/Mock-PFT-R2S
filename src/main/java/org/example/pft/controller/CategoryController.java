package org.example.pft.controller;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.example.pft.dto.category.CategoryByTypeData;
import org.example.pft.dto.category.CategoryData;
import org.example.pft.dto.category.CategoryRequest;
import org.example.pft.dto.category.CategoryResponse;
import org.example.pft.dto.category.CreateCategoryData;
import org.example.pft.enums.CategoryType;
import org.example.pft.service.CategoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/categories")
@AllArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;

    @GetMapping(params = "!type")
    public ResponseEntity<CategoryResponse<Map<CategoryType, List<CategoryData>>>> getAll(){
        return ResponseEntity.ok().body(categoryService.getAll());
    }

    @GetMapping(params = "type")
    public ResponseEntity<CategoryResponse<List<CategoryByTypeData>>> getByType(@RequestParam String type){
        return ResponseEntity.ok().body(categoryService.getByType(type));
    }

    @PostMapping
    public ResponseEntity<CategoryResponse<CreateCategoryData>> create(@RequestBody @Valid CategoryRequest request){
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.create(request));
    }
}
