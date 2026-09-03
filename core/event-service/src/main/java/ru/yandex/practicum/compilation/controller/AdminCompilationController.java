package ru.yandex.practicum.compilation.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.compilation.service.CompilationService;
import ru.yandex.practicum.dto.compilation.CompilationDto;
import ru.yandex.practicum.dto.compilation.NewCompilationDto;
import ru.yandex.practicum.dto.compilation.UpdateCompilationRequest;
import ru.yandex.practicum.feigns.main.compilation.AdminCompilationFeign;


@RestController
@RequestMapping("/admin/compilations")
@RequiredArgsConstructor
@Slf4j
@Validated
public class AdminCompilationController implements AdminCompilationFeign {

    private final CompilationService compilationService;

    @Override
    public CompilationDto createCompilation(@Valid @RequestBody NewCompilationDto dto) {
        log.info("API Администратора: Запрос на создание подборки '{}'", dto.getTitle());
        return compilationService.createCompilation(dto);
    }

    @Override
    public CompilationDto updateCompilation(
            @Positive @PathVariable Long compId,
            @Valid @RequestBody UpdateCompilationRequest request) {
        log.info("API Администратора: Запрос на обновление подборки с ID={}", compId);
        return compilationService.updateCompilation(compId, request);
    }

    @Override
    public void deleteCompilation(@Positive @PathVariable Long compId) {
        log.info("API Администратора: Запрос на удаление подборки с ID={}", compId);
        compilationService.deleteCompilation(compId);
    }
}
