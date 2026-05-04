package org.example.QuanLyMuaVu.module.inventory.repository;

import org.example.QuanLyMuaVu.module.inventory.entity.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Integer> {

    @Query("""
            SELECT s FROM Supplier s
            WHERE (:q IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :q, '%')))
            ORDER BY s.name
            """)
    Page<Supplier> searchByName(@Param("q") String q, Pageable pageable);
}
