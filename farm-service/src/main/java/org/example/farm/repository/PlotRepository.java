package org.example.farm.repository;

import java.util.List;
import java.util.Optional;
import org.example.farm.entity.Farm;
import org.example.farm.entity.Plot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PlotRepository extends JpaRepository<Plot, Integer> {
    List<Plot> findByPlotNameContainingIgnoreCase(String name);

    List<Plot> findAllByFarm(Farm farm);

    List<Plot> findAllByFarm_Id(Integer farmId);

    boolean existsByFarm(Farm farm);

    boolean existsByCreatedByAndPlotNameIgnoreCase(Long createdBy, String plotName);

    @Query("SELECT p FROM Plot p WHERE p.id = :plotId AND p.farm.userId = :ownerId")
    Optional<Plot> findByIdAndFarmUserId(@Param("plotId") Integer plotId, @Param("ownerId") Long ownerId);

    @Query("SELECT p FROM Plot p WHERE p.farm.userId = :ownerId")
    List<Plot> findAllByFarmUserId(@Param("ownerId") Long ownerId);

    @Query("SELECT p FROM Plot p WHERE p.farm.userId = :ownerId AND p.farm.id = :farmId")
    List<Plot> findAllByFarmUserIdAndFarmId(@Param("ownerId") Long ownerId, @Param("farmId") Integer farmId);

    @Query("SELECT COUNT(p) > 0 FROM Plot p WHERE p.id = :plotId AND p.farm.userId = :ownerId")
    boolean existsByIdAndFarmUserId(@Param("plotId") Integer plotId, @Param("ownerId") Long ownerId);

    @Query("SELECT COUNT(p) FROM Plot p WHERE p.farm.userId = :ownerId")
    long countByFarmUserId(@Param("ownerId") Long ownerId);
}
