package org.example.QuanLyMuaVu.module.identity.repository;

import java.util.Optional;
import org.example.QuanLyMuaVu.module.identity.entity.UserPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserPreferenceRepository extends JpaRepository<UserPreference, Integer> {
    Optional<UserPreference> findByUser_Id(Long userId);
}
