package com.orderlink.integration;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderlink.AbstractIntegrationTest;
import com.orderlink.config.LogisticsClientConfig;
import com.orderlink.dto.OrderRequest;
import com.orderlink.fixtures.TestFixtures;
import com.orderlink.logistic.client.LateLogisticsClient;
import com.orderlink.logistic.client.model.ShipmentRequest;
import com.orderlink.order.repository.InventoryEntryRepository;

@Import({LogisticsClientConfig.class})
@AutoConfigureMockMvc
public class OrderControllerIntegrationTest extends AbstractIntegrationTest     {
        
    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private InventoryEntryRepository inventoryEntryRepository;

    @MockBean
    private LateLogisticsClient lateLogisticsClient;

    @BeforeEach
    void cleanRepository() {
        inventoryEntryRepository.deleteAll();
        Mockito.reset(lateLogisticsClient);
    }

    @Test
    void createOrderPersistsInventoryEntryAndTriggersShipment() throws Exception {
        OrderRequest orderRequest = TestFixtures.sampleOrderRequest();
        String merchantRef = orderRequest.merchantRef();

        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(orderRequest)))
                .andExpect(status().isAccepted());

        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(inventoryEntryRepository.findByOrderRequest_MerchantRef(merchantRef))
                        .isPresent());

        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(
                        () -> verify(lateLogisticsClient, atLeast(3)).createShipment(anyString(), any(ShipmentRequest.class)));
    }
}