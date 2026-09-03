package ru.yandex.practicum.rating.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.dto.events.EventFullDto;
import ru.yandex.practicum.dto.user.UserDto;
import ru.yandex.practicum.enums.EventState;
import ru.yandex.practicum.error.exception.ConflictException;
import ru.yandex.practicum.error.exception.NotFoundException;
import ru.yandex.practicum.event.service.EventAdditionalService;
import ru.yandex.practicum.feigns.user.UserAdminFeign;
import ru.yandex.practicum.rating.entity.Rate;
import ru.yandex.practicum.rating.repo.RateRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RateServiceImpl implements RateService {

    private final RateRepository rateRepository;
    private final UserAdminFeign userAdminFeign;
    private final EventAdditionalService eventAdditionalService;

    @Override
    public void addRate(Long userId, Long eventId, Boolean isLike) {
        log.info("Пользователь ID={} ставит {} событию ID={}", userId, isLike ? "ЛАЙК" : "ДИЗЛАЙК", eventId);

        UserDto user = getUserById(userId);
        EventFullDto event = getEventById(eventId);

        if (!event.getState().equals(EventState.PUBLISHED)) {
            throw new ConflictException("Нельзя оценивать неопубликованные события");
        }

        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Инициатор не может оценивать собственное событие");
        }

        // Ищем существующую оценку. Если есть — обновляем, если нет — создаем.
        Rate rate = rateRepository.findByEventIdAndUserId(eventId, userId)
                .orElse(Rate.builder()
                        .user(userId)
                        .event(eventId)
                        .build());

        rate.setIsLike(isLike);
        rateRepository.save(rate);
    }

    @Override
    public void deleteRate(Long userId, Long eventId) {
        log.info("Пользователь ID={} удаляет оценку у события ID={}", userId, eventId);

        // Проверяем, существует ли пользователь и событие
        getUserById(userId);
        getEventById(eventId);

        Rate rate = rateRepository.findByEventIdAndUserId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Оценка пользователя ID=" + userId + " для события ID=" + eventId + " не найдена"));

        rateRepository.delete(rate);
    }

    public List<Object[]> getRatingsForEvents(List<Long> eventIds) {
        return rateRepository.getRatingsForEvents(eventIds);
    }

    private UserDto getUserById(Long id) {
        UserDto user = userAdminFeign.getById(id);
        if (user == null) throw new NotFoundException("Пользователь с ID " + id + " не найден");
        return user;
    }

    private EventFullDto getEventById(Long id) {
        return eventAdditionalService.findEventById(id);

    }
}
