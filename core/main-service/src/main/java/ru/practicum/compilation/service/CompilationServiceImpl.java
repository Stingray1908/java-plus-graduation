package ru.practicum.compilation.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.StatsClient;
import ru.practicum.compilation.entity.Compilation;
import ru.practicum.compilation.mapper.CompilationMapper;
import ru.practicum.compilation.repo.CompilationRepository;
import ru.practicum.dto.ViewStats;
import ru.practicum.rating.repo.RateRepository;
import ru.yandex.practicum.dto.compilation.CompilationDto;
import ru.yandex.practicum.dto.compilation.NewCompilationDto;
import ru.yandex.practicum.dto.compilation.UpdateCompilationRequest;
import ru.yandex.practicum.dto.events.EventShortDto;
import ru.yandex.practicum.enums.EventState;
import ru.yandex.practicum.error.exception.NotFoundException;
import ru.yandex.practicum.feigns.event.EventsAdminFeign;
import ru.yandex.practicum.feigns.request.RequestAdditionalFeign;
import ru.yandex.practicum.feigns.request.RequestPrivateFeign;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service

@Slf4j
@Transactional(readOnly = true)
public class CompilationServiceImpl implements CompilationService {

    private final CompilationRepository compilationRepository;
    private final RequestPrivateFeign requestPrivateFeign;
    private final RequestAdditionalFeign requestAdditionalFeign;
    private final EventsAdminFeign eventsAdminFeign;
    private final RateRepository rateRepository;
    private final StatsClient statsClient;

    public CompilationServiceImpl(CompilationRepository compilationRepository,
                                  RequestPrivateFeign requestPrivateFeign,
                                  RequestAdditionalFeign requestAdditionalFeign, EventsAdminFeign eventsAdminFeign,
                                  RateRepository rateRepository,
                                  @Qualifier("StatsClientDiscovery") StatsClient statsClient) {
        this.compilationRepository = compilationRepository;
        this.requestPrivateFeign = requestPrivateFeign;
        this.requestAdditionalFeign = requestAdditionalFeign;
        this.eventsAdminFeign = eventsAdminFeign;
        this.rateRepository = rateRepository;
        this.statsClient = statsClient;
    }

    /**
     * Создает новую подборку событий.
     *
     * @param dto DTO с данными для создания подборки
     * @return созданная подборка с заполненной статистикой просмотров и подтвержденных запросов
     */
    @Transactional
    @Override
    public CompilationDto createCompilation(NewCompilationDto dto) {
        log.info("Создание новой подборки: {}", dto.getTitle());

        List<Long> events = new ArrayList<>();

        /*if (dto.getEvents() == null && dto.getEvents().isEmpty()) {
            //events = eventsAdminFeign.getEventsByIds(dto.getEvents());
        }*/

        Compilation compilation = CompilationMapper.toCompilation(dto, events);
        Compilation saved = compilationRepository.save(compilation);

        return mapToDtoWithStats(saved);
    }

    /**
     * Удаляет подборку по идентификатору.
     *
     * @param compId идентификатор подборки
     * @throws NotFoundException если подборка с указанным идентификатором не найдена
     */
    @Transactional
    @Override
    public void deleteCompilation(Long compId) {
        log.info("Удаление подборки с ID: {}", compId);
        long deletedRows = compilationRepository.deleteCompilationById(compId);
        if (deletedRows == 0) throw new NotFoundException("Compilation with id=" + compId + " was not found");
    }

    /**
     * Обновляет данные подборки.
     *
     * @param compId  идентификатор подборки
     * @param request запрос с данными для обновления
     * @return обновленная подборка с заполненной статистикой
     * @throws NotFoundException если подборка с указанным идентификатором не найдена
     */
    @Transactional
    @Override
    public CompilationDto updateCompilation(Long compId, UpdateCompilationRequest request) {
        log.info("Обновление подборки с ID: {}", compId);
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Compilation with id=" + compId + " was not found"));

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            compilation.setTitle(request.getTitle());
        }

        if (request.getPinned() != null) {
            compilation.setPinned(request.getPinned());
        }

        if (request.getEvents() != null) {
            if (request.getEvents().isEmpty()) {
                compilation.setEventIds(new ArrayList<>());
            } else {
                compilation.setEventIds(request.getEvents());
            }
        }

        Compilation updated = compilationRepository.save(compilation);
        return mapToDtoWithStats(updated);
    }

    /**
     * Получает список подборок с пагинацией и опциональной фильтрацией по признаку закрепления.
     *
     * @param pinned фильтр по признаку закрепления (null - без фильтрации)
     * @param from   индекс первого элемента для пагинации
     * @param size   количество элементов на странице
     * @return список подборок с заполненной статистикой
     */
    @Override
    public List<CompilationDto> getCompilations(Boolean pinned, Integer from, Integer size) {
        log.info("Получение списка подборок (pinned={}, from={}, size={})", pinned, from, size);
        PageRequest pageRequest = PageRequest.of(from / size, size);

        List<Compilation> compilations;
        if (pinned != null) {
            compilations = compilationRepository.findAllByPinned(pinned, pageRequest);
        } else {
            compilations = compilationRepository.findAll(pageRequest).getContent();
        }

        return mapToDtoListWithStats(compilations);
    }

    /**
     * Получает подборку по идентификатору.
     *
     * @param compId идентификатор подборки
     * @return подборка с заполненной статистикой
     * @throws NotFoundException если подборка с указанным идентификатором не найдена
     */
    @Override
    public CompilationDto getCompilationById(Long compId) {
        log.info("Получение подборки с ID: {}", compId);
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Compilation with id=" + compId + " was not found"));

        return mapToDtoWithStats(compilation);
    }


    /**
     * Преобразует подборку в DTO с заполненной статистикой.
     *
     * @param compilation подборка для преобразования
     * @return DTO с данными подборки и статистикой
     */
    private CompilationDto mapToDtoWithStats(Compilation compilation) {
        return mapToDtoListWithStats(List.of(compilation)).getFirst();
    }

    /**
     * Преобразует список подборок в список DTO с заполненной статистикой.
     *
     * @param compilations список подборок для преобразования
     * @return список DTO с данными подборок и статистикой
     */
    private List<CompilationDto> mapToDtoListWithStats(List<Compilation> compilations) {
        List<Long> allEvents = compilations.stream()
                .flatMap(c -> c.getEventIds().stream())
                .distinct()
                .collect(Collectors.toList());

        Map<Long, EventShortDto> eventDtoMap;

        if (!allEvents.isEmpty()) {
            List<EventShortDto> shortDtos = eventsAdminFeign.getEventShortDtoByIdsWithStats(allEvents);
            eventDtoMap = shortDtos.stream()
                    .collect(Collectors.toMap(EventShortDto::getId, dto -> dto));
        } else {
            eventDtoMap = Map.of();
        }

        return compilations.stream()
                .map(compilation -> {
                    List<EventShortDto> events = compilation.getEventIds().stream()
                            .map(eventDtoMap::get)
                            .filter(Objects::nonNull)
                            .toList();

                    return CompilationMapper.toCompilationDto(compilation, events);
                })
                .toList();
    }

    /*private List<EventShortDto> mapToEventShortDto(Compilation compilation, Map<Long, Long> confirmedRequests,
                                                   Map<Long, Long> views, Map<Long, Long> ratings) {
        List<EventShortDto> shortEvents = compilation.getEventIds().stream()
                .map(event -> {
                    EventShortDto shortDto = EventsMapper.toShortEventDto(
                            event,
                            confirmedRequestsMap.getOrDefault(event.getId(), 0L),
                            ratingsMap.getOrDefault(event.getId(), 0L)
                    );
                    shortDto.setViews(viewsMap.getOrDefault(event.getId(), 0L));
                    return shortDto;
                })
    }*/

    private Map<Long, Long> getRatingsMap(List<Long> eventIds) {
        if (eventIds.isEmpty()) return Map.of();

        List<Object[]> results = rateRepository.getRatingsForEvents(eventIds);
        return results.stream().collect(Collectors.toMap(
                row -> ((Number) row[0]).longValue(),
                row -> ((Number) row[1]).longValue()
        ));
    }

    /**
     * Получает карту количества подтвержденных запросов для списка событий.
     *
     * @param events список событий
     * @return карта, где ключ - идентификатор события, значение - количество подтвержденных запросов
     */
    private Map<Long, Long> getConfirmedRequestsMap(List<Long> events) {
        if (events.isEmpty()) return Map.of();

        List<Object[]> results = requestAdditionalFeign.countRequestsByEventIdsAndStatus(events, EventState.CONFIRMED);

        return results.stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),
                        row -> ((Number) row[1]).longValue()
                ));
    }

    /**
     * Получает карту количества просмотров для списка событий из сервиса статистики.
     *
     * @param events список событий
     * @return карта, где ключ - идентификатор события, значение - количество просмотров
     */
    private Map<Long, Long> getViewsMap(List<Long> events) {
        if (events.isEmpty()) return Map.of();

        List<String> uris = events.stream()
                .map(e -> "/events/" + e)
                .collect(Collectors.toList());

        LocalDateTime start = LocalDateTime.now().minusYears(10);
        LocalDateTime end = LocalDateTime.now();

        List<ViewStats> stats;
        try {
            stats = statsClient.getStats(start, end, uris, true);
        } catch (Exception e) {
            log.error("Ошибка при получении статистики", e);
            return Map.of();
        }

        Map<Long, Long> viewsMap = new HashMap<>();
        for (ViewStats stat : stats) {
            String uri = stat.getUri();
            if (uri.startsWith("/events/")) {
                try {
                    Long eventId = Long.parseLong(uri.substring("/events/".length()));
                    viewsMap.put(eventId, stat.getHits());
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return viewsMap;
    }
}
