package ru.practicum.compilation.mapper;

import ru.practicum.compilation.entity.Compilation;
import ru.yandex.practicum.dto.compilation.CompilationDto;
import ru.yandex.practicum.dto.compilation.NewCompilationDto;
import ru.yandex.practicum.dto.events.EventShortDto;

import java.util.ArrayList;
import java.util.List;

public class CompilationMapper {

    public static Compilation toCompilation(NewCompilationDto dto, List<Long> events) {
        Compilation compilation = new Compilation();
        compilation.setTitle(dto.getTitle());
        compilation.setPinned(dto.getPinned() != null ? dto.getPinned() : false);
        compilation.setEventIds(events);
        return compilation;
    }

    public static CompilationDto toCompilationDto(Compilation compilation, List<EventShortDto> shortDtos) {
        return CompilationDto.builder()
                .id(compilation.getId())
                .title(compilation.getTitle())
                .pinned(compilation.getPinned())
                .events(shortDtos == null ? new ArrayList<>() : shortDtos)
                .build();
    }

}
