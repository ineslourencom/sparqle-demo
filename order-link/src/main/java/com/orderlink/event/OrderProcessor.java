package com.orderlink.event;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;

public interface OrderProcessor {

    @Async
    @EventListener
    void processOrderEvent(OrderEvent event);       

}
