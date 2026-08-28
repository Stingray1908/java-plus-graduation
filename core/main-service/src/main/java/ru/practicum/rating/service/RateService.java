package ru.practicum.rating.service;

public interface RateService {
    void addRate(Long userId, Long eventId, Boolean isLike);

    void deleteRate(Long userId, Long eventId);
}