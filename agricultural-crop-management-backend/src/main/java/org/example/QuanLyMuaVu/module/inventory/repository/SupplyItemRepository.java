package org.example.QuanLyMuaVu.module.inventory.repository;

import org.example.QuanLyMuaVu.module.inventory.entity.SupplyItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SupplyItemRepository extends JpaRepository<SupplyItem, Integer> {

    @Query("""
            SELECT i FROM SupplyItem i
            WHERE (:q IS NULL OR LOWER(i.name) LIKE LOWER(CONCAT('%', :q, '%')))
              AND (:restricted IS NULL OR i.restrictedFlag = :restricted)
            ORDER BY i.name
            """)
    Page<SupplyItem> searchItems(
            @Param("q") String q,
            @Param("restricted") Boolean restricted,
            Pageable pageable);
}
