package ru.yandex.practicum.controller;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.yandex.practicum.grpc.recommendation.*;
import ru.yandex.practicum.service.RecommendationsService;
import ru.yandex.practicum.service.RecommendationsService.ScoredEvent;

import java.util.List;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class RecommendationsController extends RecommendationsControllerGrpc.RecommendationsControllerImplBase {

    private final RecommendationsService recommendationsService;

    @Override
    public void getRecommendationsForUser(
            UserPredictionsRequestProto request,
            StreamObserver<RecommendedEventResponse> responseObserver) {

        long userId = request.getUserId();
        int maxResults = request.getMaxResults() > 0 ? request.getMaxResults() : 10;

        log.info("gRPC getRecommendationsForUser: userId={}, maxResults={}", userId, maxResults);

        List<ScoredEvent> recommendations =
                recommendationsService.getRecommendationsForUser(userId, maxResults);

        for (ScoredEvent rec : recommendations) {
            responseObserver.onNext(RecommendedEventResponse.newBuilder()
                    .setEventId(rec.eventId())
                    .setScore(rec.score())
                    .build());
        }

        responseObserver.onCompleted();
    }

    @Override
    public void getSimilarEvents(
            SimilarEventsRequestProto request,
            StreamObserver<RecommendedEventResponse> responseObserver) {

        long eventId = request.getEventId();
        long userId = request.getUserId();
        int maxResults = request.getMaxResults() > 0 ? request.getMaxResults() : 10;

        log.info("gRPC getSimilarEvents: eventId={}, userId={}, maxResults={}",
                eventId, userId, maxResults);

        List<ScoredEvent> similar =
                recommendationsService.getSimilarEvents(eventId, userId, maxResults);

        for (ScoredEvent rec : similar) {
            responseObserver.onNext(RecommendedEventResponse.newBuilder()
                    .setEventId(rec.eventId())
                    .setScore(rec.score())
                    .build());
        }

        responseObserver.onCompleted();
    }

    @Override
    public void getInteractionsCount(
            InteractionsCountRequestProto request,
            StreamObserver<RecommendedEventResponse> responseObserver) {

        List<Long> eventIds = request.getEventIdList();

        log.info("gRPC getInteractionsCount: eventIds={}", eventIds);

        List<ScoredEvent> counts =
                recommendationsService.getInteractionsCount(eventIds);

        for (ScoredEvent rec : counts) {
            responseObserver.onNext(RecommendedEventResponse.newBuilder()
                    .setEventId(rec.eventId())
                    .setScore(rec.score())
                    .build());
        }

        responseObserver.onCompleted();
    }
}
