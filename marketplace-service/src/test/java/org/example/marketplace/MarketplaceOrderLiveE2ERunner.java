package org.example.marketplace;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class MarketplaceOrderLiveE2ERunner {

    private static final String DB_URL = "jdbc:mysql://localhost:3307/marketplace_db?useSSL=false&allowPublicKeyRetrieval=true";
    private static final String IDENTITY_DB_URL = "jdbc:mysql://localhost:3307/identity_db?useSSL=false&allowPublicKeyRetrieval=true";
    private static final String DB_USER = "springuser";
    private static final String DB_PASS = "springpass";
    private static final String GATEWAY_URL = "http://localhost:8000";

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final HttpClient httpClient = HttpClient.newHttpClient();

    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println("STARTING LIVE E2E SMOKE TEST FOR MARKETPLACE ORDER LIFECYCLE");
        System.out.println("===============================================================");

        try {
            // 1. Resolve User IDs from identity_db
            Map<String, Long> userIds = resolveUserIds();
            Long buyerId = userIds.getOrDefault("buyer", 4L);
            Long farmerId = userIds.getOrDefault("farmer", 2L);
            Long adminId = userIds.getOrDefault("admin", 1L);

            System.out.println("Resolved User IDs -> Buyer: " + buyerId + ", Farmer: " + farmerId + ", Admin: " + adminId);

            // 2. Perform Buyer Cancel E2E Test
            runBuyerCancelE2ETest(buyerId, farmerId);

            // 3. Perform Farmer Complete E2E Test
            runFarmerCompleteE2ETest(buyerId, farmerId);

            // 4. Perform Admin Complete E2E Test
            runAdminCompleteE2ETest(buyerId, farmerId);

            System.out.println("===============================================================");
            System.out.println("ALL LIVE E2E SMOKE TESTS PASSED SUCCESSFULLY!");
            System.out.println("===============================================================");
            System.exit(0);

        } catch (Throwable e) {
            System.err.println("===============================================================");
            System.err.println("E2E SMOKE TEST FAILED!");
            System.err.println("===============================================================");
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static Map<String, Long> resolveUserIds() throws Exception {
        Map<String, Long> userIds = new HashMap<>();
        try (Connection conn = DriverManager.getConnection(IDENTITY_DB_URL, DB_USER, DB_PASS)) {
            String sql = "SELECT user_id, user_name FROM users WHERE user_name IN ('buyer', 'farmer', 'admin')";
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    userIds.put(rs.getString("user_name"), rs.getLong("user_id"));
                }
            }
        }
        return userIds;
    }

    private static void runBuyerCancelE2ETest(Long buyerId, Long farmerId) throws Exception {
        System.out.println("\n--- [TEST 1] Buyer Order Cancellation ---");

        // Insert fresh order
        long orderGroupId = insertOrderGroup(buyerId);
        String orderCode = "ORD-LIVE-CANCEL-" + System.currentTimeMillis();
        long orderId = insertOrder(orderGroupId, orderCode, buyerId, farmerId, "PENDING_PAYMENT");
        System.out.println("Created test order in DB: ID=" + orderId + ", Code=" + orderCode);

        // Login as Buyer
        String buyerToken = login("buyer", "12345678");
        System.out.println("Logged in as Buyer. Token received.");

        // Send cancel request to Gateway
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GATEWAY_URL + "/api/v1/marketplace/orders/" + orderId + "/cancel"))
                .header("Authorization", "Bearer " + buyerToken)
                .PUT(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("Gateway Response Status: " + response.statusCode());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Cancel request failed: " + response.body());
        }

        // Wait for outbox publisher container to scan and publish
        System.out.println("Waiting 2 seconds for background event propagation...");
        Thread.sleep(2000);

        // Verify state in DB
        verifyOrderStateAndOutbox(orderId, "CANCELLED", "MarketplaceOrderCancelledEvent");
        System.out.println("[PASS] Buyer Cancellation E2E Test Succeeded!");
    }

    private static void runFarmerCompleteE2ETest(Long buyerId, Long farmerId) throws Exception {
        System.out.println("\n--- [TEST 2] Farmer Order Completion ---");

        // Insert fresh order under SHIPPED status
        long orderGroupId = insertOrderGroup(buyerId);
        String orderCode = "ORD-LIVE-FARMER-COMP-" + System.currentTimeMillis();
        long orderId = insertOrder(orderGroupId, orderCode, buyerId, farmerId, "SHIPPED");
        System.out.println("Created test order in DB: ID=" + orderId + ", Code=" + orderCode);

        // Login as Farmer
        String farmerToken = login("farmer", "12345678");
        System.out.println("Logged in as Farmer. Token received.");

        // Send completion request to Gateway
        Map<String, String> bodyMap = Map.of("status", "COMPLETED");
        String body = objectMapper.writeValueAsString(bodyMap);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GATEWAY_URL + "/api/v1/farmer/orders/" + orderId + "/status"))
                .header("Authorization", "Bearer " + farmerToken)
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("Gateway Response Status: " + response.statusCode());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Farmer status update failed: " + response.body());
        }

        // Wait for outbox publisher container to scan and publish
        System.out.println("Waiting 2 seconds for background event propagation...");
        Thread.sleep(2000);

        // Verify state in DB
        verifyOrderStateAndOutbox(orderId, "COMPLETED", "MarketplaceOrderCompletedEvent");
        System.out.println("[PASS] Farmer Completion E2E Test Succeeded!");
    }

    private static void runAdminCompleteE2ETest(Long buyerId, Long farmerId) throws Exception {
        System.out.println("\n--- [TEST 3] Admin Order Completion ---");

        // Insert fresh order under DELIVERED status
        long orderGroupId = insertOrderGroup(buyerId);
        String orderCode = "ORD-LIVE-ADMIN-COMP-" + System.currentTimeMillis();
        long orderId = insertOrder(orderGroupId, orderCode, buyerId, farmerId, "DELIVERED");
        System.out.println("Created test order in DB: ID=" + orderId + ", Code=" + orderCode);

        // Login as Admin
        String adminToken = login("admin", "admin123");
        System.out.println("Logged in as Admin. Token received.");

        // Send completion request to Gateway
        Map<String, String> bodyMap = Map.of("status", "COMPLETED");
        String body = objectMapper.writeValueAsString(bodyMap);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GATEWAY_URL + "/api/v1/marketplace/admin/orders/" + orderId + "/status"))
                .header("Authorization", "Bearer " + adminToken)
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("Gateway Response Status: " + response.statusCode());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Admin status update failed: " + response.body());
        }

        // Wait for outbox publisher container to scan and publish
        System.out.println("Waiting 2 seconds for background event propagation...");
        Thread.sleep(2000);

        // Verify state in DB
        verifyOrderStateAndOutbox(orderId, "COMPLETED", "MarketplaceOrderCompletedEvent");
        System.out.println("[PASS] Admin Completion E2E Test Succeeded!");
    }

    private static String login(String username, String password) throws Exception {
        Map<String, Object> credentials = Map.of(
                "identifier", username,
                "password", password,
                "rememberMe", false
        );
        String requestBody = objectMapper.writeValueAsString(credentials);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GATEWAY_URL + "/api/v1/auth/sign-in"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Login failed for user " + username + ": " + response.body());
        }

        JsonNode jsonNode = objectMapper.readTree(response.body());
        return jsonNode.get("result").get("token").asText();
    }

    private static long insertOrderGroup(Long buyerId) throws Exception {
        String groupCode = "GRP-" + System.currentTimeMillis();
        String idempotencyKey = "KEY-" + System.currentTimeMillis();
        String sql = "INSERT INTO marketplace_order_groups (group_code, buyer_user_id, idempotency_key, total_amount, status, request_fingerprint) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, groupCode);
            stmt.setLong(2, buyerId);
            stmt.setString(3, idempotencyKey);
            stmt.setBigDecimal(4, new java.math.BigDecimal("110.00"));
            stmt.setString(5, "PENDING");
            stmt.setString(6, "fingerprint");
            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                } else {
                    throw new SQLException("Creating order group failed, no ID obtained.");
                }
            }
        }
    }

    private static long insertOrder(long groupId, String orderCode, Long buyerId, Long farmerId, String status) throws Exception {
        String sql = "INSERT INTO marketplace_orders " +
                "(order_group_id, order_code, buyer_user_id, farmer_user_id, status, payment_method, payment_verification_status, " +
                "shipping_recipient_name, shipping_phone, shipping_address_line, subtotal, shipping_fee, total_amount) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, groupId);
            stmt.setString(2, orderCode);
            stmt.setLong(3, buyerId);
            stmt.setLong(4, farmerId);
            stmt.setString(5, status);
            stmt.setString(6, "BANK_TRANSFER");
            stmt.setString(7, "AWAITING_PROOF");
            stmt.setString(8, "John Doe");
            stmt.setString(9, "0903234000");
            stmt.setString(10, "123 Main St");
            stmt.setBigDecimal(11, new java.math.BigDecimal("100.00"));
            stmt.setBigDecimal(12, new java.math.BigDecimal("10.00"));
            stmt.setBigDecimal(13, new java.math.BigDecimal("110.00"));
            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                } else {
                    throw new SQLException("Creating order failed, no ID obtained.");
                }
            }
        }
    }

    private static void verifyOrderStateAndOutbox(long orderId, String expectedStatus, String expectedEventType) throws Exception {
        // 1. Verify status in DB
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            String sql = "SELECT status FROM marketplace_orders WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, orderId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String status = rs.getString("status");
                        if (!expectedStatus.equals(status)) {
                            throw new RuntimeException("DB order status mismatch! Expected: " + expectedStatus + ", Actual: " + status);
                        }
                        System.out.println("Verified order status in DB: " + status);
                    } else {
                        throw new RuntimeException("Order not found with ID: " + orderId);
                    }
                }
            }

            // 2. Verify OutboxEvent is processed (with retry polling up to 10s)
            String outboxSql = "SELECT event_type, processed, payload FROM outbox_events WHERE aggregate_type = 'MarketplaceOrder' AND aggregate_id = ?";
            long startTime = System.currentTimeMillis();
            boolean verified = false;
            String lastError = "";

            while (System.currentTimeMillis() - startTime < 10000) {
                try (PreparedStatement stmt = conn.prepareStatement(outboxSql)) {
                    stmt.setString(1, String.valueOf(orderId));
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            String eventType = rs.getString("event_type");
                            boolean processed = rs.getBoolean("processed");
                            String payload = rs.getString("payload");

                            if (!expectedEventType.equals(eventType)) {
                                throw new RuntimeException("Outbox Event Type mismatch! Expected: " + expectedEventType + ", Actual: " + eventType);
                            }
                            if (processed) {
                                System.out.println("Verified Outbox Event: " + eventType + " (processed=" + processed + ")");
                                System.out.println("Payload preview: " + payload.substring(0, Math.min(100, payload.length())) + "...");
                                verified = true;
                                break;
                            } else {
                                lastError = "Outbox event exists but not processed yet.";
                            }
                        } else {
                            lastError = "No outbox event found for order ID: " + orderId;
                        }
                    }
                }
                Thread.sleep(500);
            }

            if (!verified) {
                throw new RuntimeException("Outbox processing verification failed after 10s timeout: " + lastError);
            }
        }
    }
}
