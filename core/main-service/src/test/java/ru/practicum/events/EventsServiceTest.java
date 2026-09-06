package ru.practicum.events;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.StatsClient;
import ru.practicum.categories.entity.Category;
import ru.practicum.categories.repo.CategoryRepository;
import ru.yandex.practicum.dto.events.EventFullDto;
import ru.yandex.practicum.dto.events.Location;
import ru.yandex.practicum.dto.events.NewEventDto;
import ru.yandex.practicum.error.exception.EventCreationRuleException;
import ru.yandex.practicum.error.exception.NotFoundException;
import ru.practicum.events.service.EventsService;
import ru.practicum.user.User;
import ru.practicum.user.UserRepository;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
@Rollback
class EventsServiceTest {

    @Autowired
    private EventsService eventsService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @MockBean(name = "StatsClientDiscovery")
    private StatsClient statsClient;

    private User user;
    private Category category;

    @BeforeEach
    void setUp() {
        // Создаём тестового пользователя
        user = User.builder()
                .name("Test User")
                .email("test@user.com")
                .build();
        user = userRepository.save(user);

        // Создаём тестовую категорию
        category = Category.builder()
                .name("Test Category")
                .build();
        category = categoryRepository.save(category);
    }


    @Test
    void shouldSaveEventSuccessfully() {
        // Given
        NewEventDto newEventDto = NewEventDto.builder()
                .annotation("Test annotation")
                .category(category.getId())
                .description("Test description")
                .eventDate(LocalDateTime.now().plusDays(2))
                .title("Test Event")
                .paid(false)
                .participantLimit(10)
                .requestModeration(true)
                .location(new Location(55.75f,37.62f))
                .build();

        // When
        EventFullDto result = eventsService.saveEvent(newEventDto, user.getId());

        // Then
        assertNotNull(result);
        assertEquals("Test Event", result.getTitle());
        assertEquals("Test annotation", result.getAnnotation());
        assertNotNull(result.getCategory());
        assertEquals(category.getId(), result.getCategory().getId());
        assertEquals(category.getName(), result.getCategory().getName());
        assertEquals(user.getId(), result.getInitiator().getId());
    }


    @Test
    void shouldThrowExceptionWhenCategoryNotFound() {
        // Given
        Long nonExistentCategoryId = 999L;
        NewEventDto newEventDto = NewEventDto.builder()
                .annotation("Test annotation")
                .category(nonExistentCategoryId)
                .description("Test description")
                .eventDate(LocalDateTime.now().plusDays(2))
                .title("Test Event")
                .paid(false)
                .participantLimit(10)
                .requestModeration(true)
                .build();

        // When & Then
        EventCreationRuleException exception = assertThrows(
                EventCreationRuleException.class,
                () -> eventsService.saveEvent(newEventDto, user.getId())
        );

        assertTrue(exception.getMessage().contains("Категория с ID " + nonExistentCategoryId + " не найдена"));
        assertEquals("categoryId", exception.getField());
        assertEquals(nonExistentCategoryId, exception.getRejectedValue());
    }

    @Test
    void shouldThrowExceptionWhenEventDateTooSoon() {
        // Given
        LocalDateTime tooSoonDate = LocalDateTime.now().plusHours(1); // Меньше MIN_HOURS_BEFORE_EVENT

        NewEventDto newEventDto = NewEventDto.builder()
                .annotation("Test annotation")
                .category(category.getId())
                .description("Test description")
                .eventDate(tooSoonDate)
                .title("Test Event")
                .paid(false)
                .participantLimit(10)
                .requestModeration(true)
                .build();

        // When & Then
        EventCreationRuleException exception = assertThrows(
                EventCreationRuleException.class,
                () -> eventsService.saveEvent(newEventDto, user.getId())
        );

        assertTrue(exception.getMessage().contains("Событие не удовлетворяет правилам создания"));
        assertEquals("eventDate", exception.getField());
        assertEquals(tooSoonDate, exception.getRejectedValue());
    }


    @Test
    void shouldThrowExceptionWhenUserNotFound() {
        // Given
        Long nonExistentUserId = 999L;

        NewEventDto newEventDto = NewEventDto.builder()
                .annotation("Test annotation")
                .category(category.getId())
                .description("Test description")
                .eventDate(LocalDateTime.now().plusDays(2))
                .title("Test Event")
                .paid(false)
                .participantLimit(10)
                .requestModeration(true)
                .build();

        // When & Then
        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> eventsService.saveEvent(newEventDto, nonExistentUserId)
        );

        assertTrue(exception.getMessage().contains("Пользователь с ID " + nonExistentUserId + " не найден"));
    }

    @Test
    void shouldSaveAllEventFieldsCorrectly() {
        // Given
        Location locationDto = new Location(55.123456f, 37.987654f);
        NewEventDto newEventDto = NewEventDto.builder()
                .annotation("Full test annotation")
                .category(category.getId())
                .description("Full test description with all details")
                .eventDate(LocalDateTime.now().plusDays(5))
                .title("Full Test Event")
                .paid(true)
                .participantLimit(50)
                .requestModeration(false)
                .location(locationDto)
                .build();

        // When
        EventFullDto result = eventsService.saveEvent(newEventDto, user.getId());

        // Then
        assertEquals("Full test annotation", result.getAnnotation());
        assertEquals("Full test description with all details", result.getDescription());
        assertEquals("Full Test Event", result.getTitle());
        assertTrue(result.getPaid());
        assertEquals(50, result.getParticipantLimit());
        assertFalse(result.getRequestModeration());
        assertNotNull(result.getLocation());
        assertEquals(55.123456, result.getLocation().getLat(), 0.000009);
        assertEquals(37.987654, result.getLocation().getLon(), 0.000009);
    }
}
