package ru.yandex.practicum.config;

import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

public class EventSimilarityAvroDeserializer extends BaseAvroDeserializer<EventSimilarityAvro> {
    public EventSimilarityAvroDeserializer() {
            super(UserActionAvro.getClassSchema());
        }
    }
