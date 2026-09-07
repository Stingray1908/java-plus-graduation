package ru.yandex.practicum.rest;

import stats.service.collector.UserActionControllerGrpc.UserActionControllerImplBase;
import stats.service.collector.UserActionProtoOuterClass.UserActionProto;
import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.kafka.core.KafkaTemplate;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Instant;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class UserActionControllerImpl extends UserActionControllerImplBase {

    private static final String TOPIC = "stats.actions.v1";

    private final KafkaTemplate<String, UserActionAvro> kafkaTemplate;

    @Override
    public void collectUserAction(UserActionProto request, StreamObserver<Empty> responseObserver) {
        log.info("Получено действие пользователя: userId={}, eventId={}, actionType={}",
                request.getUserId(), request.getEventId(), request.getActionType());

        UserActionAvro avro = UserActionAvro.newBuilder()
                .setUserId(request.getUserId())
                .setEventId(request.getEventId())
                .setActionType(mapActionType(request.getActionType()))
                .setTimestamp(mapTimestamp(request.getTimestamp()))
                .build();

        kafkaTemplate.send(TOPIC, String.valueOf(request.getUserId()), avro);

        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    private ActionTypeAvro mapActionType(
            stats.service.collector.UserActionProtoOuterClass.ActionTypeProto protoType) {
        return switch (protoType) {
            case ACTION_VIEW -> ActionTypeAvro.VIEW;
            case ACTION_REGISTER -> ActionTypeAvro.REGISTER;
            case ACTION_LIKE -> ActionTypeAvro.LIKE;
            default -> throw new IllegalArgumentException("Неизвестный тип действия: " + protoType);
        };
    }

    private Instant mapTimestamp(com.google.protobuf.Timestamp protoTimestamp) {
        return Instant.ofEpochSecond(protoTimestamp.getSeconds(), protoTimestamp.getNanos());
    }
}
