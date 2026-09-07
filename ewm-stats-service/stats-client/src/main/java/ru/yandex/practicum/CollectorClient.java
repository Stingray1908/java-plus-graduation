package ru.yandex.practicum;

import com.google.protobuf.Empty;
import com.google.protobuf.Timestamp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import stats.service.collector.UserActionControllerGrpc;
import stats.service.collector.UserActionProtoOuterClass;


import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class CollectorClient {

    @GrpcClient("grpcCollector")
    private UserActionControllerGrpc.UserActionControllerBlockingStub blockingStub;

    public void sendUserAction(long userId, long eventId, UserActionProtoOuterClass.ActionTypeProto actionType, Instant timestamp) {
        UserActionProtoOuterClass.UserActionProto request = UserActionProtoOuterClass.UserActionProto.newBuilder()
                .setUserId(userId)
                .setEventId(eventId)
                .setActionType(actionType)
                .setTimestamp(Timestamp.newBuilder()
                        .setSeconds(timestamp.getEpochSecond())
                        .setNanos(timestamp.getNano())
                        .build())
                .build();

        Empty response = blockingStub.collectUserAction(request);
        log.info("Действие отправлено в Collector: userId={}, eventId={}", userId, eventId);
    }
}
