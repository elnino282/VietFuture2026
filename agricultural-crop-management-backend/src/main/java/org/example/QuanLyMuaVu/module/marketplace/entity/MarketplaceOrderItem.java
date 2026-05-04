package org.example.QuanLyMuaVu.module.marketplace.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "marketplace_order_items")
public class MarketplaceOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    Long id;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    MarketplaceOrder order;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    MarketplaceProduct product;

    @Column(name = "product_name_snapshot", nullable = false)
    String productNameSnapshot;

    @Column(name = "product_slug_snapshot", nullable = false, length = 191)
    String productSlugSnapshot;

    @Column(name = "image_url_snapshot", length = 1024)
    String imageUrlSnapshot;

    @Column(name = "unit_price_snapshot", nullable = false, precision = 19, scale = 2)
    BigDecimal unitPriceSnapshot;

    @Column(name = "quantity", nullable = false, precision = 19, scale = 3)
    BigDecimal quantity;

    @Column(name = "line_total", nullable = false, precision = 19, scale = 2)
    BigDecimal lineTotal;

    @Column(name = "traceable_snapshot", nullable = false)
    Boolean traceableSnapshot;

    @ManyToOne
    @JoinColumn(name = "farm_id")
    org.example.QuanLyMuaVu.module.farm.entity.Farm farm;

    @ManyToOne
    @JoinColumn(name = "season_id")
    org.example.QuanLyMuaVu.module.season.entity.Season season;

    @ManyToOne
    @JoinColumn(name = "lot_id")
    org.example.QuanLyMuaVu.module.inventory.entity.ProductWarehouseLot lot;
}
