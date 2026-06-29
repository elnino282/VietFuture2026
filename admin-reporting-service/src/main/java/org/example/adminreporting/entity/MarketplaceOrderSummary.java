package org.example.adminreporting.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "admin_marketplace_order_summary")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketplaceOrderSummary {
    @Id
    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "order_code")
    private String orderCode;

    @Column(name = "buyer_id")
    private Long buyerId;

    @Column(name = "buyer_name")
    private String buyerName;

    @Column(name = "total_amount", precision = 15, scale = 4)
    private BigDecimal totalAmount;

    @Column(name = "payment_status")
    private String paymentStatus;

    @Column(name = "status")
    private String status;

    @Column(name = "payment_proof_uploaded_at")
    private LocalDateTime paymentProofUploadedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
