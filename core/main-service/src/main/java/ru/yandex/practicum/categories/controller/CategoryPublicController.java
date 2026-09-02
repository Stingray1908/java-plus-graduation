package ru.yandex.practicum.categories.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.categories.service.CategoryService;
import ru.yandex.practicum.dto.categories.CategoryDto;
import ru.yandex.practicum.feigns.main.category.CategoryPublicFeign;

import java.util.List;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryPublicController implements CategoryPublicFeign {

    private final CategoryService categoryService;

    @Override
    public ResponseEntity<List<CategoryDto>> getCategories(
            @RequestParam(defaultValue = "0") Integer from,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        List<CategoryDto> categories = categoryService.getCategories(from, size);
        return ResponseEntity.ok(categories);
    }

    @Override
    public ResponseEntity<CategoryDto> getCategoryById(@PathVariable Long catId) {
        CategoryDto category = categoryService.getCategoryById(catId);
        return ResponseEntity.ok(category);
    }
}
