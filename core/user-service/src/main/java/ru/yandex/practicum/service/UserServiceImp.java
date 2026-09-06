package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.dto.user.NewUserRequest;
import ru.yandex.practicum.dto.user.UserDto;
import ru.yandex.practicum.dto.user.UserShortDto;
import ru.yandex.practicum.entity.User;
import ru.yandex.practicum.error.exception.ConflictException;
import ru.yandex.practicum.error.exception.NotFoundException;
import ru.yandex.practicum.mapper.UserMapper;
import ru.yandex.practicum.repo.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class UserServiceImp implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public UserDto save(NewUserRequest request) {
        log.info("Начинаем создание нового пользователя: {}", request.getName());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("User with email " + request.getEmail() + " already exists");
        }

        User user = userRepository.save(userMapper.toEntity(request));
        log.info("Пользователь успешно создан с ID: {}", user.getId());
        return userMapper.toDto(user);
    }

    @Transactional(readOnly = true)
    @Override
    public List<UserDto> findByIdsOrAllWithPagination(List<Long> ids, int offset, int size) {
        List<User> users;
        log.debug("Получен запрос на получение пользователей. IDs: {}, offset: {}, size: {}", ids, offset, size);

        if (ids != null && !ids.isEmpty()) {

            users = userRepository.findByIds(ids);
            log.debug("Найдено {} пользователей по указанным ID", users.size());
        } else {

            users = userRepository.findAllWithOffset(offset, size);
            log.debug("Найдено {} пользователей без фильтрации по ID", users.size());
        }

        List<UserDto> result = users.stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());

        log.info("Возвращаем {} пользователей", result.size());
        return result;
    }

    @Override
    public void deleteById(Long id) {
        log.info("Начинаем удаление пользователя с ID: {}", id);
        if (userRepository.deleteByIdAndReturnRow(id) == 0) {
            log.warn("Попытка удаления несуществующего пользователя с ID: {}", id);
            throw new NotFoundException("Пользователь с id:" + id + " не существует");
        }
        log.info("Пользователь с ID {} успешно удалён", id);
    }

    @Override
    public List<UserShortDto> getAllInIds(List<Long> ids) {
        return userRepository.findByIds(ids).stream()
                .map(userMapper::toShortDto)
                .toList();
    }

    @Override
    public UserShortDto getById(Long userId) {
        return userMapper.toShortDto(userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден")));
    }
}
