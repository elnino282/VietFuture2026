package org.example.delivery.repository;

import org.example.delivery.entity.DeliveryRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface DeliveryRateRepository extends JpaRepository<DeliveryRate, Integer> {

    @Query("SELECT r FROM DeliveryRate r WHERE r.providerId = :providerId " +
           "AND r.zoneFrom = :zoneFrom AND r.zoneTo = :zoneTo " +
           "AND :weight >= r.weightMinKg AND :weight <= r.weightMaxKg " +
           "AND r.isColdChain = :isColdChain")
    Optional<DeliveryRate> findRate(
        @Param("providerId") Integer providerId,
        @Param("zoneFrom") String zoneFrom,
        @Param("zoneTo") String zoneTo,
        @Param("weight") BigDecimal weight,
        @Param("isColdChain") boolean isColdChain
    );
}
