package com.orderlink.notification.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.orderlink.notification.service.EventStreamService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventStreamService eventService;

    @GetMapping(path = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        SseEmitter emitter = new SseEmitter(0L);
        emitter.onCompletion(() -> eventService.removeEmitter(emitter));
        emitter.onTimeout(() -> {
            emitter.complete();
            eventService.removeEmitter(emitter);
        });
        emitter.onError(error -> {
            emitter.completeWithError(error);
            eventService.removeEmitter(emitter);
        });
        eventService.addEmitter(emitter);
        return emitter;
    }
}
