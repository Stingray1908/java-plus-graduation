package ru.yandex.practicum.event.moderation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.dto.events.moderation.ModerationCommentShortDto;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static ru.yandex.practicum.event.moderation.ModerationMapper.toListModerationCommentShortDto;

@RequiredArgsConstructor
@Service
public class ModerationService {
    private final ModerationCommentRepository moderationCommentRepository;

    public ModerationCommentShortDto getCommentById(Long id) {

        return moderationCommentRepository.findLastCommentsByEventIds(List.of(id)).stream()
                .map(ModerationMapper::toModerationCommentShortDto)
                .findFirst()
                .orElse(null);
    }

    public Map<Long, ModerationCommentShortDto> getCommentsMap(List<Long> ids) {

        List<ModerationCommentShortDto> comments = toListModerationCommentShortDto(
                moderationCommentRepository.findLastCommentsByEventIds(ids));

        return comments.stream()
                .collect(Collectors.toMap(
                        ModerationCommentShortDto::getId,
                        Function.identity()
                ));
    }

    public ModerationComment save(ModerationComment mc) {
        return moderationCommentRepository.save(mc);
    }
}
