package org.example.QuanLyMuaVu.module.identity.service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.module.identity.entity.Role;
import org.example.QuanLyMuaVu.module.identity.entity.User;
import org.example.QuanLyMuaVu.module.identity.port.IdentityCommandPort;
import org.example.QuanLyMuaVu.module.identity.repository.PasswordResetTokenRepository;
import org.example.QuanLyMuaVu.module.identity.repository.RoleRepository;
import org.example.QuanLyMuaVu.module.identity.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class IdentityCommandService implements IdentityCommandPort {

    UserRepository userRepository;
    RoleRepository roleRepository;
    PasswordResetTokenRepository passwordResetTokenRepository;

    @Override
    public boolean existsByUsername(String username) {
        if (username == null || username.isBlank()) {
            return false;
        }
        return userRepository.existsByUsername(username);
    }

    @Override
    public boolean existsByEmailIgnoreCase(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return userRepository.existsByEmailIgnoreCase(email);
    }

    @Override
    public Optional<User> findUserById(Long userId) {
        if (userId == null) {
            return Optional.empty();
        }
        return userRepository.findById(userId);
    }

    @Override
    public User saveUser(User user) {
        return userRepository.save(user);
    }

    @Override
    public Optional<Role> findRoleByCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        return roleRepository.findByCode(code);
    }

    @Override
    public List<Role> findRolesByCodes(Collection<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return List.of();
        }
        return roleRepository.findByCodeIn(codes);
    }

    @Override
    public void deletePasswordResetTokensByUserId(Long userId) {
        if (userId == null) {
            return;
        }
        passwordResetTokenRepository.deleteByUserId(userId);
    }

    @Override
    public void deleteUser(User user) {
        if (user == null) {
            return;
        }
        userRepository.delete(user);
    }
}
