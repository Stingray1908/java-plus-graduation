package ru.yandex.practicum;

import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.recommendation.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Slf4j
@Component
public class AnalyzerClient {

    @GrpcClient("analyzer")
    private RecommendationsControllerGrpc.RecommendationsControllerBlockingStub blockingStub;

    /**
     * GET /events/recommendations
     */
    public List<ScoredEvent> getRecommendationsForUser(long userId, int maxResults) {
        UserPredictionsRequestProto request = UserPredictionsRequestProto.newBuilder()
                .setUserId(userId)
                .setMaxResults(maxResults)
                .build();

        return collectResponses(
                blockingStub.getRecommendationsForUser(request)
        );
    }

    /**
     * GET /events/{eventId}/similar
     */
    public List<ScoredEvent> getSimilarEvents(long eventId, long userId, int maxResults) {
        SimilarEventsRequestProto request = SimilarEventsRequestProto.newBuilder()
                .setEventId(eventId)
                .setUserId(userId)
                .setMaxResults(maxResults)
                .build();

        return collectResponses(
                blockingStub.getSimilarEvents(request)
        );
    }

    /**
     * Подстановка rating в мероприятия
     */
    public List<ScoredEvent> getInteractionsCount(List<Long> eventIds) {
        InteractionsCountRequestProto request = InteractionsCountRequestProto.newBuilder()
                .addAllEventId(eventIds)
                .build();

        return collectResponses(
                blockingStub.getInteractionsCount(request)
        );
    }

    private List<ScoredEvent> collectResponses(Iterator<RecommendedEventResponse> responses) {
        List<ScoredEvent> result = new ArrayList<>();
        while (responses.hasNext()) {
            RecommendedEventResponse resp = responses.next();
            result.add(new ScoredEvent(resp.getEventId(), resp.getScore()));
        }
        return result;
    }

    public record ScoredEvent(long eventId, double score) {}
}
