package org.example.season.repository;

import org.example.season.entity.PesticidePHIReference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface PesticidePHIReferenceRepository extends JpaRepository<PesticidePHIReference, Integer> {

    Optional<PesticidePHIReference> findByPesticideNameContainingIgnoreCase(String pesticideName);

    Optional<PesticidePHIReference> findByActiveIngredientContainingIgnoreCase(String activeIngredient);

    @Query("SELECT r FROM PesticidePHIReference r WHERE " +
           "LOWER(r.pesticideName) LIKE LOWER(CONCAT('%', :name, '%')) OR " +
           "LOWER(r.activeIngredient) LIKE LOWER(CONCAT('%', :name, '%'))")
    Optional<PesticidePHIReference> findByName(@Param("name") String name);
}
