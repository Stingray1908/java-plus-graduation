package ru.yandex.practicum.categories.controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.categories.service.CategoryService;
import ru.yandex.practicum.dto.categories.CategoryDto;
import ru.yandex.practicum.feigns.main.category.CategoryAdminFeign;

@RestController
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
@Slf4j
public class CategoryAdminController implements CategoryAdminFeign {

    private final CategoryService categoryService;

    @Override
    public ResponseEntity<CategoryDto> createCategory(@Valid @RequestBody CategoryDto dto) {
        CategoryDto created = categoryService.createCategory(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Override
    public ResponseEntity<CategoryDto> updateCategory(
            @PathVariable Long catId,
            @Valid @RequestBody CategoryDto dto
    ) {
        CategoryDto updated = categoryService.updateCategory(catId, dto);
        return ResponseEntity.ok(updated);
    }

    @Override
    public void deleteCategory(@PathVariable Long catId) {
        log.info("Получен запрос на удаление категории ID:{}", catId);
        categoryService.deleteCategory(catId);
    }
}
