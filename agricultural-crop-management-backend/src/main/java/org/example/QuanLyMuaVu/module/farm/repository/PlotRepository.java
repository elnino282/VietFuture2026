package org.example.QuanLyMuaVu.module.farm.repository;

import java.util.List;
import java.util.Optional;
import org.example.QuanLyMuaVu.module.farm.entity.Farm;
import org.example.QuanLyMuaVu.module.farm.entity.Plot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PlotRepository extends JpaRepository<Plot, Integer> {
    List<Plot> findByPlotNameContainingIgnoreCase(String name);

    List<Plot> findAllByUser(org.example.QuanLyMuaVu.module.identity.entity.User user);

    List<Plot> findAllByFarm(Farm farm);

    List<Plot> findAllByFarm_Id(Integer farmId);

    boolean existsByFarm(Farm farm);

    boolean existsByUserAndPlotNameIgnoreCase(org.example.QuanLyMuaVu.module.identity.entity.User user, String plotName);

    /**
     * Find plot by ID only if the farm owner matches.
     * Used for ownership verification: plots.farm_id -> farms.owner_id
     * 
     * @param plotId  the plot ID
     * @param ownerId the expected farm owner's user ID
     * @return Optional containing the plot if found and owned
     */
    @Query("SELECT p FROM Plot p WHERE p.id = :plotId AND p.farm.user.id = :ownerId")
    Optional<Plot> findByIdAndFarmUserId(@Param("plotId") Integer plotId, @Param("ownerId") Long ownerId);

    /**
     * Find all plots for farms owned by the specified user.
     * 
     * @param ownerId the farm owner's user ID
     * @return list of plots for farms owned by the user
     */
    @Query("SELECT p FROM Plot p WHERE p.farm.user.id = :ownerId")
    List<Plot> findAllByFarmUserId(@Param("ownerId") Long ownerId);

    @Query("SELECT p FROM Plot p WHERE p.farm.user.id = :ownerId AND p.farm.id = :farmId")
    List<Plot> findAllByFarmUserIdAndFarmId(@Param("ownerId") Long ownerId, @Param("farmId") Integer farmId);

    /**
     * Check if a plot exists and is owned by the specified user (via farm).
     * 
     * @param plotId  the plot ID
     * @param ownerId the expected farm owner's user ID
     * @return true if plot exists and farm is owned by user
     */
    @Query("SELECT COUNT(p) > 0 FROM Plot p WHERE p.id = :plotId AND p.farm.user.id = :ownerId")
    boolean existsByIdAndFarmUserId(@Param("plotId") Integer plotId, @Param("ownerId") Long ownerId);

    /**
     * Count plots by farm owner ID.
     * Used for dashboard plots count.
     */
    @Query("SELECT COUNT(p) FROM Plot p WHERE p.farm.user.id = :ownerId")
    long countByFarmUserId(@Param("ownerId") Long ownerId);
}
