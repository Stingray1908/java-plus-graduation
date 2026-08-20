package ru.practicum.subscriptions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.categories.Category;
import ru.practicum.categories.CategoryRepository;
import ru.practicum.events.Event;
import ru.practicum.events.EventState;
import ru.practicum.events.EventsRepository;
import ru.practicum.user.User;
import ru.practicum.user.UserRepository;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback
@AutoConfigureTestDatabase
class PrivateSubscriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private EventsRepository eventsRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    private User subscriber;
    private User publisher;
    private Category category;

    @BeforeEach
    void setUp() {
        subscriber = userRepository.save(User.builder()
                .name("Subscriber")
                .email("subscriber@example.com")
                .build());

        publisher = userRepository.save(User.builder()
                .name("Publisher")
                .email("publisher@example.com")
                .build());

        category = categoryRepository.save(Category.builder()
                .name("Subscription Category")
                .build());
    }

    @Test
    void shouldSubscribeUserToPublisher() throws Exception {
        mockMvc.perform(post("/users/{userId}/subscriptions/{publisherId}", subscriber.getId(), publisher.getId()))
                .andExpect(status().isCreated());

        assertTrue(subscriptionRepository.existsBySubscriber_IdAndPublisher_Id(subscriber.getId(), publisher.getId()));
    }

    @Test
    void shouldUnsubscribeUserFromPublisher() throws Exception {
        subscriptionRepository.save(new Subscription(null, subscriber, publisher, LocalDateTime.now()));

        mockMvc.perform(delete("/users/{userId}/subscriptions/{publisherId}", subscriber.getId(), publisher.getId()))
                .andExpect(status().isNoContent());

        assertFalse(subscriptionRepository.existsBySubscriber_IdAndPublisher_Id(subscriber.getId(), publisher.getId()));
    }

    @Test
    void shouldReturnConflictWhenUserSubscribesToHimself() throws Exception {
        mockMvc.perform(post("/users/{userId}/subscriptions/{publisherId}", subscriber.getId(), subscriber.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value(containsString("cannot subscribe to himself")));
    }

    @Test
    void shouldReturnOnlyActualPublishedEventsFromSubscribedPublishersSortedByEventDateDesc() throws Exception {
        User secondPublisher = userRepository.save(User.builder()
                .name("Second Publisher")
                .email("second.publisher@example.com")
                .build());
        User notSubscribedPublisher = userRepository.save(User.builder()
                .name("Not Subscribed Publisher")
                .email("not.subscribed@example.com")
                .build());

        subscriptionRepository.save(new Subscription(null, subscriber, publisher, LocalDateTime.now()));
        subscriptionRepository.save(new Subscription(null, subscriber, secondPublisher, LocalDateTime.now()));

        Event olderSubscribedEvent = eventsRepository.save(createEvent(
                "Older subscribed event",
                publisher,
                EventState.PUBLISHED,
                LocalDateTime.now().plusDays(2)));
        Event newerSubscribedEvent = eventsRepository.save(createEvent(
                "Newer subscribed event",
                secondPublisher,
                EventState.PUBLISHED,
                LocalDateTime.now().plusDays(4)));
        eventsRepository.save(createEvent(
                "Future event from non subscribed publisher",
                notSubscribedPublisher,
                EventState.PUBLISHED,
                LocalDateTime.now().plusDays(5)));
        eventsRepository.save(createEvent(
                "Past event from subscribed publisher",
                publisher,
                EventState.PUBLISHED,
                LocalDateTime.now().minusDays(1)));
        eventsRepository.save(createEvent(
                "Pending event from subscribed publisher",
                publisher,
                EventState.PENDING,
                LocalDateTime.now().plusDays(6)));

        mockMvc.perform(get("/users/{userId}/subscriptions/events", subscriber.getId())
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(newerSubscribedEvent.getId()))
                .andExpect(jsonPath("$[0].title").value("Newer subscribed event"))
                .andExpect(jsonPath("$[1].id").value(olderSubscribedEvent.getId()))
                .andExpect(jsonPath("$[1].title").value("Older subscribed event"));
    }

    private Event createEvent(String title, User initiator, EventState state, LocalDateTime eventDate) {
        return Event.builder()
                .title(title)
                .annotation("Annotation for " + title)
                .description("Description for " + title)
                .initiator(initiator)
                .state(state)
                .eventDate(eventDate)
                .category(category)
                .paid(false)
                .participantLimit(10)
                .requestModeration(true)
                .locationLat(55.75f)
                .locationLon(37.62f)
                .confirmedRequests(0L)
                .createdOn(LocalDateTime.now())
                .publishedOn(state == EventState.PUBLISHED ? LocalDateTime.now() : null)
                .views(0L)
                .build();
    }
}
