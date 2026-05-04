package org.example.QuanLyMuaVu.module.identity.controller;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.DTO.Common.ApiResponse;
import org.example.QuanLyMuaVu.module.identity.dto.request.PreferencesUpdateRequest;
import org.example.QuanLyMuaVu.module.identity.dto.response.PreferencesResponse;
import org.example.QuanLyMuaVu.module.identity.service.PreferencesService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/preferences")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PreferencesController {

    PreferencesService preferencesService;

    @PreAuthorize("hasAnyRole('ADMIN','BUYER','FARMER','EMPLOYEE')")
    @GetMapping("/me")
    public ApiResponse<PreferencesResponse> getMyPreferences() {
        return ApiResponse.success(preferencesService.getMyPreferences());
    }

    @PreAuthorize("hasAnyRole('ADMIN','BUYER','FARMER','EMPLOYEE')")
    @PutMapping("/me")
    public ApiResponse<PreferencesResponse> updateMyPreferences(@RequestBody PreferencesUpdateRequest request) {
        return ApiResponse.success(preferencesService.updateMyPreferences(request));
    }
}
