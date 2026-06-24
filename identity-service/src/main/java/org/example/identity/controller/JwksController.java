package org.example.identity.controller;

import com.nimbusds.jose.jwk.JWKSet;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.example.identity.config.RsaKeyProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the JSON Web Key Set (JWKS) endpoint so that other services
 * (monolith, crop-catalog, etc.) can fetch the public key to verify
 * JWT tokens signed by the identity-service.
 */
@RestController
@RequiredArgsConstructor
public class JwksController {

    private final RsaKeyProvider rsaKeyProvider;

    @GetMapping("/api/v1/auth/.well-known/jwks.json")
    public Map<String, Object> jwks() {
        JWKSet jwkSet = new JWKSet(rsaKeyProvider.getPublicJwk());
        return jwkSet.toJSONObject();
    }
}
