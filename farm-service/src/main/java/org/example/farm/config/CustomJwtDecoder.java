package org.example.farm.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CustomJwtDecoder implements JwtDecoder {

    @Value("${jwt.jwks-uri}")
    private String jwksUri;

    private volatile NimbusJwtDecoder rsaDecoder = null;

    @Override
    public Jwt decode(String token) throws JwtException {
        try {
            return getRsaDecoder().decode(token);
        } catch (Exception e) {
            log.error("JWT decoding failed: {}", e.getMessage());
            throw new JwtException("JWT decode failed: " + e.getMessage());
        }
    }

    private NimbusJwtDecoder getRsaDecoder() {
        if (rsaDecoder == null) {
            synchronized (this) {
                if (rsaDecoder == null) {
                    log.info("Initializing RS256 JWKS-based JWT Decoder with URI: {}", jwksUri);
                    rsaDecoder = NimbusJwtDecoder.withJwkSetUri(jwksUri)
                            .jwsAlgorithm(SignatureAlgorithm.RS256)
                            .build();
                }
            }
        }
        return rsaDecoder;
    }
}
