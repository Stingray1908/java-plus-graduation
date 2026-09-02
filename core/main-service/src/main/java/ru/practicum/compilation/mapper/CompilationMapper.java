package ru.practicum.compilation.mapper;

import ru.yandex.practicum.dto.compilation.CompilationDto;
import ru.yandex.practicum.dto.compilation.NewCompilationDto;
import ru.practicum.compilation.entity.Compilation;
import ru.practicum.events.entity.Event;
import ru.practicum.events.mapper.EventsMapper;
import ru.yandex.practicum.dto.events.EventShortDto;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CompilationMapper {

    public static Compilation toCompilation(NewCompilationDto dto, List<Long> events) {
        Compilation compilation = new Compilation();
        compilation.setTitle(dto.getTitle());
        compilation.setPinned(dto.getPinned() != null ? dto.getPinned() : false);
        compilation.setEventIds(events);
        return compilation;
    }

    public static CompilationDto toCompilationDto(
            Compilation compilation,
            Map<Long, Long> confirmedRequestsMap,
            Map<Long, Long> viewsMap,
            Map<Long, Long> ratingsMap) {

        List<EventShortDto> shortEvents = compilation.getEvents().stream()
                .map(event -> {
                    EventShortDto shortDto = EventsMapper.toShortEventDto(
                            event,
                            confirmedRequestsMap.getOrDefault(event.getId(), 0L),
                            ratingsMap.getOrDefault(event.getId(), 0L)
                    );
                    shortDto.setViews(viewsMap.getOrDefault(event.getId(), 0L));
                    return shortDto;
                })
                .collect(Collectors.toList());

        return CompilationDto.builder()
                .id(compilation.getId())
                .title(compilation.getTitle())
                .pinned(compilation.getPinned())
                .events(shortEvents)
                .build();
    }

}
