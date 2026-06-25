package org.example.farm.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.farm.dto.response.ProvinceResponse;
import org.example.farm.dto.response.WardResponse;
import org.example.farm.service.AddressService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/locations")
@RequiredArgsConstructor
public class LocationController {

    private final AddressService addressService;

    @GetMapping("/provinces")
    public ResponseEntity<List<ProvinceResponse>> getAllProvinces() {
        return ResponseEntity.ok(addressService.getAllProvinces(null, null));
    }

    @GetMapping("/wards")
    public ResponseEntity<List<WardResponse>> getWardsByProvince(@RequestParam Integer provinceId) {
        return ResponseEntity.ok(addressService.getWardsByProvinceId(provinceId, null));
    }
}
