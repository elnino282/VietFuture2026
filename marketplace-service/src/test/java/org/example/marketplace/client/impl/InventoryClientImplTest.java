package org.example.marketplace.client.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.example.marketplace.client.InventoryClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

class InventoryClientImplTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private HttpServer server;
    private InventoryClientImpl inventoryClient;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.start();

        inventoryClient = new InventoryClientImpl(WebClient.builder());
        ReflectionTestUtils.setField(
                inventoryClient,
                "inventoryServiceUrl",
                "http://localhost:" + server.getAddress().getPort());
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("reserveStock sends real item unit to inventory reservation API")
    void reserveStock_sendsUnitInReservationPayload() {
        AtomicReference<String> idempotencyHeader = new AtomicReference<>();
        AtomicReference<JsonNode> requestJson = new AtomicReference<>();

        server.createContext("/api/v1/inventory/reservations/reserve", exchange -> {
            idempotencyHeader.set(exchange.getRequestHeaders().getFirst("X-Idempotency-Key"));
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            requestJson.set(objectMapper.readTree(body));

            byte[] response = """
                    {
                      "reservationId": 10,
                      "idempotencyKey": "checkout-1",
                      "orderId": 99,
                      "status": "RESERVED",
                      "message": "Stock reserved successfully",
                      "items": [
                        {
                          "itemId": 123,
                          "lotId": 500,
                          "lotCode": "LOT-500",
                          "quantity": 2.5,
                          "unit": "box",
                          "previousOnHand": 20,
                          "newOnHand": 20
                        }
                      ]
                    }
                    """.getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });

        InventoryClient.ReservationResult result = inventoryClient.reserveStock(
                "checkout-1",
                99L,
                List.of(new InventoryClient.ReserveItem(
                        123L,
                        500,
                        "LOT-500",
                        new BigDecimal("2.5"),
                        "box")));

        assertThat(result.success()).isTrue();
        assertThat(idempotencyHeader.get()).isEqualTo("checkout-1");
        assertThat(requestJson.get().path("idempotencyKey").asText()).isEqualTo("checkout-1");
        assertThat(requestJson.get().path("orderId").asLong()).isEqualTo(99L);
        assertThat(requestJson.get().path("items").get(0).path("orderItemId").asLong()).isEqualTo(123L);
        assertThat(requestJson.get().path("items").get(0).path("lotId").asInt()).isEqualTo(500);
        assertThat(requestJson.get().path("items").get(0).path("quantity").decimalValue())
                .isEqualByComparingTo(new BigDecimal("2.5"));
        assertThat(requestJson.get().path("items").get(0).path("unit").asText()).isEqualTo("box");
    }
}
