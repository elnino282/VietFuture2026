package org.example.delivery.repository;

import org.example.delivery.entity.DeliveryProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryProviderRepository extends JpaRepository<DeliveryProvider, Integer> {
    List<DeliveryProvider> findByIsActiveTrue();
    Optional<DeliveryProvider> findByCode(String code);
}
