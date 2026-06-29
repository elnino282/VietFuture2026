package org.example.adminreporting.dto.event;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class AdminEvents {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FarmChangedEventDto {
        private Integer farmId;
        private Long userId;
        private String farmName;
        private String provinceId;
        private String provinceName;
        private String wardId;
        private String wardName;
        private BigDecimal area;
        private Double latitude;
        private Double longitude;
        private Boolean active;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlotChangedEventDto {
        private Integer plotId;
        private Integer farmId;
        private String plotName;
        private BigDecimal area;
        private String soilType;
        private String boundaryGeoJson;
        private String status;
        private String action;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SeasonChangedEventDto {
        private Integer seasonId;
        private String seasonName;
        private Integer plotId;
        private Integer cropId;
        private Integer farmId;
        private Integer varietyId;
        private LocalDate startDate;
        private LocalDate plannedHarvestDate;
        private LocalDate endDate;
        private String status;
        private Integer initialPlantCount;
        private Integer currentPlantCount;
        private BigDecimal expectedYieldKg;
        private BigDecimal actualYieldKg;
        private BigDecimal budgetAmount;
        private String notes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HarvestRecordedEventDto {
        private Integer harvestId;
        private Integer seasonId;
        private Integer farmId;
        private LocalDate harvestDate;
        private BigDecimal quantity;
        private String unit;
        private String grade;
        private String note;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExpenseChangedEventDto {
        private Integer expenseId;
        private Long userId;
        private Integer seasonId;
        private Integer taskId;
        private Integer plotId;
        private Integer farmId;
        private String category;
        private String itemName;
        private BigDecimal unitPrice;
        private BigDecimal quantity;
        private BigDecimal totalCost;
        private BigDecimal amount;
        private String paymentStatus;
        private String note;
        private LocalDate expenseDate;
        private String seasonName;
        private String plotName;
        private String taskTitle;
        private String userName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IncidentChangedEventDto {
        private Integer incidentId;
        private Integer seasonId;
        private Integer farmId;
        private Long reporterUserId;
        private String incidentType;
        private String severity;
        private String description;
        private String status;
        private LocalDate deadline;
        private LocalDateTime resolvedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductWarehouseLotReceivedEventDto {
        private Integer lotId;
        private Integer harvestId;
        private Integer seasonId;
        private Integer farmId;
        private Integer warehouseId;
        private BigDecimal quantity;
        private String unit;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MarketplaceOrderCreatedEventDto {
        private String eventId;
        private String aggregateType;
        private String aggregateId;
        private LocalDateTime occurredAt;
        private Payload payload;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Payload {
            private Long orderGroupId;
            private Long orderId;
            private Long buyerUserId;
            private Long farmerUserId;
            private String status;
            private List<OrderItemPayload> items;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class OrderItemPayload {
            private Long itemId;
            private Long productId;
            private String productName;
            private String lotCode;
            private Integer farmId;
            private String farmName;
            private Integer seasonId;
            private String seasonName;
            private Integer quantity;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MarketplacePaymentSubmittedEventDto {
        private String eventId;
        private String aggregateType;
        private String aggregateId;
        private LocalDateTime occurredAt;
        private Payload payload;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Payload {
            private Long orderId;
            private Long buyerUserId;
            private String paymentMethod;
            private String paymentProofUrl;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MarketplacePaymentVerifiedEventDto {
        private String eventId;
        private String aggregateType;
        private String aggregateId;
        private LocalDateTime occurredAt;
        private Payload payload;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Payload {
            private Long orderId;
            private Long buyerUserId;
            private Long verifiedByUserId;
            private String verificationStatus;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MarketplaceOrderCompletedEventDto {
        private String eventId;
        private String aggregateType;
        private String aggregateId;
        private LocalDateTime occurredAt;
        private Payload payload;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Payload {
            private Long orderId;
            private Long orderGroupId;
            private Long buyerUserId;
            private Long farmerUserId;
            private String completedAt;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MarketplaceOrderCancelledEventDto {
        private String eventId;
        private String aggregateType;
        private String aggregateId;
        private LocalDateTime occurredAt;
        private Payload payload;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Payload {
            private Long orderId;
            private Long orderGroupId;
            private Long buyerUserId;
            private Long cancelledByUserId;
            private String reason;
        }
    }
}
