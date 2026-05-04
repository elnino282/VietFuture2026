package org.example.QuanLyMuaVu.module.farm.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.QuanLyMuaVu.module.farm.entity.Province;
import org.example.QuanLyMuaVu.module.farm.entity.Ward;
import org.example.QuanLyMuaVu.module.farm.service.AddressService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/locations")
@RequiredArgsConstructor
public class LocationController {

    private final AddressService addressService;

    @GetMapping("/provinces")
    public ResponseEntity<List<Province>> getAllProvinces() {
        return ResponseEntity.ok(addressService.findAllProvinceEntities());
    }

    @GetMapping("/wards")
    public ResponseEntity<List<Ward>> getWardsByProvince(@RequestParam Integer provinceId) {
        return ResponseEntity.ok(addressService.findWardsByProvince(provinceId));
    }
}
