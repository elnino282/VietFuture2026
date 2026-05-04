package org.example.QuanLyMuaVu.module.identity.dto.request;

import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FarmerUpdateRequest {

    @Size(min = 8, message = "PASSWORD_INVALID")
    String password;

    List<String> roles;
}

