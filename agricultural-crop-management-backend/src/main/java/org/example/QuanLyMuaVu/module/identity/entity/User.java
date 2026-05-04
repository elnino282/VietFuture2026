package org.example.QuanLyMuaVu.module.identity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Set;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.Enums.UserStatus;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    Long id;

    @Column(name = "user_name", unique = true, columnDefinition = "VARCHAR(255) COLLATE utf8mb4_unicode_ci")
    String username;

    /**
     * Primary email of the user. This maps to the {@code email} column in the ACM
     * schema
     * and can be used for login or communication depending on business rules.
     */
    @Column(name = "email", unique = true)
    String email;

    @Column(name = "phone", length = 30)
    String phone;

    @Column(name = "full_name")
    String fullName;

    @Column(name = "password_hash")
    String password;

    /**
     * High-level status of the user account.
     * Default to ACTIVE to avoid null constraint violations.
     */
    @Builder.Default
    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(name = "status", nullable = false)
    UserStatus status = UserStatus.ACTIVE;

    @ManyToOne
    @JoinColumn(name = "province_id")
    org.example.QuanLyMuaVu.module.farm.entity.Province province;

    @ManyToOne
    @JoinColumn(name = "ward_id")
    org.example.QuanLyMuaVu.module.farm.entity.Ward ward;

    @ManyToMany(cascade = { jakarta.persistence.CascadeType.MERGE })
    @jakarta.persistence.JoinTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
    Set<Role> roles;

    @Column(name = "joined_date")
    LocalDateTime joinedDate;
}
/*
 * Vì sao không cần getter / setter? Bạn đã dùng annotation:
 *
 * @Data
 *
 * @NoArgsConstructor
 *
 * @AllArgsConstructor của Lombok.
 *
 * Cụ thể:
 *
 * @Data ⟶ Lombok sẽ tự sinh:
 *
 * getId(), getUsername()...
 *
 * setUsername(String username)...
 *
 * toString(), equals(), hashCode()
 *
 * @NoArgsConstructor ⟶ Tạo constructor không tham số: public User()
 *
 * @AllArgsConstructor ⟶ Tạo constructor đầy đủ: public User(Long id, String
 * username, ...)
 */
