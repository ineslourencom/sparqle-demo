package com.orderlink.logistic.webhook;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/webhooks")
public class WebhookController {

    private final WebhookService webhookService;

    @PostMapping("/late-logistics")
    public ResponseEntity<Void> handleLateLogisticsWebhook(@RequestBody WebhookPayload payload) {
    
        log.info("Received webhook event: {}", payload.eventId());
        webhookService.handleLateLogisticsWebhook(payload);
        return ResponseEntity.ok().build();
    }
}