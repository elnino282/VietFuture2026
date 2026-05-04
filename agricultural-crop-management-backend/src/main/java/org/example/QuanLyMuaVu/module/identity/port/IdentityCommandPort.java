package org.example.QuanLyMuaVu.module.identity.port;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.example.QuanLyMuaVu.module.identity.entity.Role;
import org.example.QuanLyMuaVu.module.identity.entity.User;

public interface IdentityCommandPort {

    boolean existsByUsername(String username);

    boolean existsByEmailIgnoreCase(String email);

    Optional<User> findUserById(Long userId);

    User saveUser(User user);

    Optional<Role> findRoleByCode(String code);

    List<Role> findRolesByCodes(Collection<String> codes);

    void deletePasswordResetTokensByUserId(Long userId);

    void deleteUser(User user);
}
