package com.orderlink.notification.service;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.orderlink.dto.OrderResponse;
import com.orderlink.event.OrderEvent;

@Service
public class EventService implements EventStreamService {

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    @Async
    @EventListener
    public void processOrderEvent(OrderEvent event) {
        OrderResponse payload = mapToResponse(event);
        emitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event().name("order-update").data(payload));
            } catch (IOException ex) {
                emitter.completeWithError(ex);
                emitters.remove(emitter);
            }
        });
    }

    @Override
    public void addEmitter(SseEmitter emitter) {
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError((ex) -> emitters.remove(emitter));
    }

    @Override
    public void removeEmitter(SseEmitter emitter) {
        emitters.remove(emitter);
    }

    private OrderResponse mapToResponse(OrderEvent event) {
        return OrderResponse.builder()
                .merchantRef(event.merchantRef())
                .state(event.order().getState())
                .details(event.order().getState().details)
                .updatedAt(event.order().getLastUpdatedAt())
                .build();
    }
}
