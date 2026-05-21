package org.example.QuanLyMuaVu.firebase;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.example.QuanLyMuaVu.Enums.UserStatus;
import org.example.QuanLyMuaVu.module.farm.entity.Farm;
import org.example.QuanLyMuaVu.module.farm.entity.Province;
import org.example.QuanLyMuaVu.module.farm.entity.Ward;
import org.example.QuanLyMuaVu.module.farm.repository.FarmRepository;
import org.example.QuanLyMuaVu.module.identity.entity.User;
import org.example.QuanLyMuaVu.module.identity.repository.UserRepository;
import org.example.QuanLyMuaVu.module.shared.security.CurrentUserService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FirebaseChatContactService {

    private static final int DEFAULT_LIMIT = 8;
    private static final int MAX_LIMIT = 20;
    private static final Pattern UID_PATTERN = Pattern.compile("^(?:u_|#)?\\s*(\\d+)\\s*$",
                    Pattern.CASE_INSENSITIVE);

    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private final FarmRepository farmRepository;

    public List<FirebaseChatContactResponse> findContacts(
                    String query,
                    List<Long> userIds,
                    Integer limitRaw) {
        Long currentUserId = currentUserService.getCurrentUserId();
        int limit = normalizeLimit(limitRaw);

        LinkedHashMap<Long, User> orderedUsers = new LinkedHashMap<>();
        boolean hasExplicitUserIds = userIds != null && !userIds.isEmpty();
        boolean hasQuery = query != null && !query.isBlank();

        if (hasExplicitUserIds) {
            addFromUserIds(orderedUsers, userIds, currentUserId);
        }

        if (hasQuery) {
            String trimmedQuery = query == null ? "" : query.trim();
            parseUserId(trimmedQuery)
                            .flatMap(userRepository::findById)
                            .ifPresent(user -> addUserIfEligible(orderedUsers, user, currentUserId));

            addFromUserKeyword(orderedUsers, trimmedQuery, limit, currentUserId);
            addFromFarmKeyword(orderedUsers, trimmedQuery, limit, currentUserId);
        }

        if (!hasExplicitUserIds && !hasQuery) {
            return List.of();
        }

        List<User> candidates = new ArrayList<>(orderedUsers.values());
        if (!hasExplicitUserIds) {
            candidates.sort(Comparator.comparing(this::displayName, String.CASE_INSENSITIVE_ORDER));
        }

        if (candidates.size() > limit) {
            candidates = candidates.subList(0, limit);
        }

        Map<Long, Farm> primaryFarmByUserId = resolvePrimaryFarms(candidates);

        return candidates.stream()
                        .map(user -> toContact(user, primaryFarmByUserId.get(user.getId())))
                        .toList();
    }

    private int normalizeLimit(Integer limitRaw) {
        if (limitRaw == null || limitRaw <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limitRaw, MAX_LIMIT);
    }

    private void addFromUserIds(
                    LinkedHashMap<Long, User> orderedUsers,
                    List<Long> userIds,
                    Long currentUserId) {
        userIds.stream()
                        .filter(id -> id != null && id > 0)
                        .distinct()
                        .map(userRepository::findById)
                        .flatMap(Optional::stream)
                        .forEach(user -> addUserIfEligible(orderedUsers, user, currentUserId));
    }

    private void addFromUserKeyword(
                    LinkedHashMap<Long, User> orderedUsers,
                    String query,
                    int limit,
                    Long currentUserId) {
        userRepository.searchByKeyword(query, PageRequest.of(0, limit))
                        .forEach(user -> addUserIfEligible(orderedUsers, user, currentUserId));
    }

    private void addFromFarmKeyword(
                    LinkedHashMap<Long, User> orderedUsers,
                    String query,
                    int limit,
                    Long currentUserId) {
        farmRepository.findByNameContainingIgnoreCase(query, PageRequest.of(0, limit))
                        .forEach(farm -> {
                            User owner = farm.getUser();
                            if (owner != null) {
                                addUserIfEligible(orderedUsers, owner, currentUserId);
                            }
                        });
    }

    private void addUserIfEligible(
                    LinkedHashMap<Long, User> orderedUsers,
                    User user,
                    Long currentUserId) {
        if (user == null || user.getId() == null) {
            return;
        }
        if (user.getId().equals(currentUserId)) {
            return;
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            return;
        }
        orderedUsers.putIfAbsent(user.getId(), user);
    }

    private Map<Long, Farm> resolvePrimaryFarms(List<User> users) {
        List<Long> userIds = users.stream()
                        .map(User::getId)
                        .filter(id -> id != null && id > 0)
                        .distinct()
                        .toList();

        if (userIds.isEmpty()) {
            return Map.of();
        }

        LinkedHashMap<Long, Farm> map = new LinkedHashMap<>();
        farmRepository.findActiveByUserIds(userIds).forEach(farm -> {
            Long ownerId = farm.getUser() == null ? null : farm.getUser().getId();
            if (ownerId != null) {
                map.putIfAbsent(ownerId, farm);
            }
        });

        return map;
    }

    private FirebaseChatContactResponse toContact(User user, Farm farm) {
        String representativeName = representativeName(user);
        return new FirebaseChatContactResponse(
                        user.getId(),
                        "u_" + user.getId(),
                        displayName(user),
                        representativeName,
                        farm == null ? null : safeTrim(farm.getName()),
                        addressOf(farm, user),
                        primaryRole(user));
    }

    private String primaryRole(User user) {
        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            return null;
        }
        return user.getRoles().stream()
                        .map(org.example.QuanLyMuaVu.module.identity.entity.Role::getCode)
                        .filter(code -> code != null && !code.isBlank())
                        .findFirst()
                        .map(code -> code.toUpperCase().startsWith("ROLE_")
                                        ? code.substring("ROLE_".length()).toUpperCase()
                                        : code.toUpperCase())
                        .orElse(null);
    }

    private String displayName(User user) {
        String fullName = safeTrim(user.getFullName());
        if (fullName != null) {
            return fullName;
        }

        String username = safeTrim(user.getUsername());
        if (username != null) {
            return username;
        }

        String email = safeTrim(user.getEmail());
        if (email != null) {
            return email;
        }

        return "User #" + user.getId();
    }

    private String representativeName(User user) {
        String fullName = safeTrim(user.getFullName());
        if (fullName != null) {
            return fullName;
        }
        String username = safeTrim(user.getUsername());
        if (username != null) {
            return username;
        }
        return "User #" + user.getId();
    }

    private String addressOf(Farm farm, User user) {
        String farmAddress = joinAddressParts(
                        safeNameWithType(farm == null ? null : farm.getWard()),
                        safeNameWithType(farm == null ? null : farm.getProvince()));
        if (farmAddress != null) {
            return farmAddress;
        }

        return joinAddressParts(
                        safeNameWithType(user.getWard()),
                        safeNameWithType(user.getProvince()));
    }

    private String safeNameWithType(Ward ward) {
        if (ward == null) {
            return null;
        }
        String value = safeTrim(ward.getNameWithType());
        return value != null ? value : safeTrim(ward.getName());
    }

    private String safeNameWithType(Province province) {
        if (province == null) {
            return null;
        }
        String value = safeTrim(province.getNameWithType());
        return value != null ? value : safeTrim(province.getName());
    }

    private String joinAddressParts(String ward, String province) {
        List<String> parts = new ArrayList<>(2);
        if (ward != null) {
            parts.add(ward);
        }
        if (province != null) {
            parts.add(province);
        }
        if (parts.isEmpty()) {
            return null;
        }
        return String.join(", ", parts);
    }

    private Optional<Long> parseUserId(String raw) {
        if (raw == null) {
            return Optional.empty();
        }

        Matcher matcher = UID_PATTERN.matcher(raw.trim());
        if (!matcher.matches()) {
            return Optional.empty();
        }

        try {
            long value = Long.parseLong(matcher.group(1).replaceFirst("^0+(?!$)", ""));
            return value > 0 ? Optional.of(value) : Optional.empty();
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    private String safeTrim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
