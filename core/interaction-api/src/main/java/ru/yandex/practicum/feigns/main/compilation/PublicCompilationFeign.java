package ru.yandex.practicum.feigns.main.compilation;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.yandex.practicum.dto.compilation.CompilationDto;

import java.util.List;

@FeignClient(name = "main-service", path = "/compilations")
@Validated
public interface PublicCompilationFeign {


    @GetMapping
    public List<CompilationDto> getCompilations(
            @RequestParam(required = false) Boolean pinned,
            @Min(0) @RequestParam(defaultValue = "0") Integer from,
            @Positive @RequestParam(defaultValue = "10") Integer size);

    @GetMapping("/{compId}")
    public CompilationDto getCompilationById(@Positive @PathVariable Long compId);
}
