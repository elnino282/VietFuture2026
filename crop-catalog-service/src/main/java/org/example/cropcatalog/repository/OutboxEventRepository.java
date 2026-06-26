package org.example.cropcatalog.repository;

import java.util.List;
import org.example.cropcatalog.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, String> {
    List<OutboxEvent> findByProcessedFalseOrderByCreatedAtAsc();
}
