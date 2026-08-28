package ru.yandex.practicum.telemetry.collector.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.telemetry.collector.model.hub.HubEvent;
import ru.yandex.practicum.telemetry.collector.model.hub.HubEventType;
import ru.yandex.practicum.telemetry.collector.model.sensor.SensorEvent;
import ru.yandex.practicum.telemetry.collector.model.sensor.SensorEventType;
import ru.yandex.practicum.telemetry.collector.service.HubEventHandler;
import ru.yandex.practicum.telemetry.collector.service.SensorEventHandler;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@RestController
@Validated
@RequestMapping(value = "/events", consumes = MediaType.APPLICATION_JSON_VALUE)
public class EventController {
    public final Map<SensorEventType, SensorEventHandler> sensorEventHandlers;
    public final Map<HubEventType, HubEventHandler> hubEventHandlers;

    public EventController(List<SensorEventHandler> sensorEventHandlers, List<HubEventHandler> hubEventHandlers) {
        this.sensorEventHandlers = sensorEventHandlers.stream()
                .collect(Collectors.toMap(SensorEventHandler::getMessageType, Function.identity()));

        this.hubEventHandlers = hubEventHandlers.stream()
                .collect(Collectors.toMap(HubEventHandler::getMessageType, Function.identity()));
    }

    @PostMapping("/sensors")
    @ResponseStatus(HttpStatus.OK)
    public void collectSensorEvent(@RequestBody @Valid SensorEvent event) {
        log.debug("Collected sensor event: {}", event);
        SensorEventHandler handler = sensorEventHandlers.get(event.getType());

        if (handler == null) {
            throw new IllegalArgumentException("No event handler found for event type " + event.getType());
        }

        handler.handle(event);
    }

    @PostMapping("/hubs")
    @ResponseStatus(HttpStatus.OK)
    public void collectHubEvent(@RequestBody @Valid HubEvent event) {
        log.debug("Collected hub event: {}", event);
        HubEventHandler handler = hubEventHandlers.get(event.getType());

        if (handler == null) {
            throw new IllegalArgumentException("No event handler found for event type " + event.getType());
        }

        handler.handle(event);
    }
}
