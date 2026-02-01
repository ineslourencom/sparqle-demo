package com.orderlink.notification.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.orderlink.event.OrderProcessor;

public interface EventStreamService extends OrderProcessor {

    void addEmitter(SseEmitter emitter);

    void removeEmitter(SseEmitter emitter);
}
