package com.orderlink.integration;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import org.awaitility.Awaitility;
import static org.hamcrest.Matchers.containsString;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import static org.mockito.Mockito.verify;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.test.util.ReflectionTestUtils;

import com.orderlink.AbstractIntegrationTest;
import com.orderlink.event.OrderEvent;
import com.orderlink.fixtures.TestFixtures;
import com.orderlink.notification.controller.EventController;
import com.orderlink.notification.service.EventService;
import com.orderlink.notification.service.EventStreamService;


@AutoConfigureMockMvc
class EventControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventController eventController;

    @SpyBean
    private EventStreamService eventStreamService;


    @BeforeEach
    void clearSpyInteractions() {
        Mockito.clearInvocations(eventStreamService);
    }

    private List<SseEmitter> emitterRegistry() {
        EventService target = AopTestUtils.getTargetObject(eventStreamService);
        
        List<SseEmitter> emitters = (List<SseEmitter>) ReflectionTestUtils.getField(target, "emitters");
        assertThat(emitters).isNotNull();
        return emitters;
    }

    private void awaitEmitterRemoval(SseEmitter emitter) {
        Awaitility.await().atMost(Duration.ofSeconds(1))
                .untilAsserted(() -> assertThat(emitterRegistry()).doesNotContain(emitter));
    }



    @Test
    void subscribeReturnsSseStreamAndRegistersEmitter() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get("/api/events/subscribe")
                .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(request().asyncStarted())
                .andReturn();

        ArgumentCaptor<SseEmitter> emitterCaptor = ArgumentCaptor.forClass(SseEmitter.class);
        verify(eventStreamService).addEmitter(emitterCaptor.capture());

        SseEmitter emitter = emitterCaptor.getValue();
        assertThat(emitter).isNotNull();
        assertThat(emitter.getTimeout()).isEqualTo(0L);

        emitter.complete();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString(MediaType.TEXT_EVENT_STREAM_VALUE)));
    }

    @Test
    @Disabled
    void emitterCompletionRemovesEmitter() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get("/api/events/subscribe")
                .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(request().asyncStarted())
                .andReturn();

        ArgumentCaptor<SseEmitter> emitterCaptor = ArgumentCaptor.forClass(SseEmitter.class);
        verify(eventStreamService).addEmitter(emitterCaptor.capture());
        SseEmitter emitter = emitterCaptor.getValue();
        assertThat(emitterRegistry()).contains(emitter);

        emitter.complete();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk());

        awaitEmitterRemoval(emitter);
    }

    @Test
    @Disabled
    void emitterErrorRemovesEmitter() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get("/api/events/subscribe")
                .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(request().asyncStarted())
                .andReturn();

        ArgumentCaptor<SseEmitter> emitterCaptor = ArgumentCaptor.forClass(SseEmitter.class);
        verify(eventStreamService).addEmitter(emitterCaptor.capture());
        SseEmitter emitter = emitterCaptor.getValue();
        assertThat(emitterRegistry()).contains(emitter);

        RuntimeException boom = new RuntimeException("boom");
        emitter.completeWithError(boom);

        assertThatThrownBy(() -> mockMvc.perform(asyncDispatch(mvcResult)))
                .hasCauseInstanceOf(RuntimeException.class);

        awaitEmitterRemoval(emitter);
    }

    @Test
    @Disabled
    void orderEventIsStreamedToClient() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get("/api/events/subscribe")
                .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(request().asyncStarted())
                .andReturn();

        ArgumentCaptor<SseEmitter> emitterCaptor = ArgumentCaptor.forClass(SseEmitter.class);
        verify(eventStreamService).addEmitter(emitterCaptor.capture());
        SseEmitter emitter = emitterCaptor.getValue();
        assertThat(emitterRegistry()).contains(emitter);

        OrderEvent orderEvent = new OrderEvent(
                emitter.hashCode() + "-merchant",
                TestFixtures.sampleOrder());

        EventService targetService = AopTestUtils.getTargetObject(eventStreamService);
        targetService.processOrderEvent(orderEvent);

        emitter.complete();
        awaitEmitterRemoval(emitter);

        MvcResult dispatched = mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString(MediaType.TEXT_EVENT_STREAM_VALUE)))
                .andReturn();

        String body = dispatched.getResponse().getContentAsString();
        assertThat(body).contains("event:order-update");
        assertThat(body).contains(orderEvent.merchantRef());
        assertThat(body).contains(orderEvent.order().getState().name());
    }
}
