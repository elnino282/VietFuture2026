package org.example.ai.config;

import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.util.Objects;

@Component
@Slf4j
public class CustomJwtDecoder implements JwtDecoder {

    @Value("${jwt.signerKey:supersecretkeyplaceholder64characterslongmustbe64byteslongforhs512!}")
    private String signerKey;

    @Value("${jwt.jwks-uri}")
    private String jwksUri;

    private volatile NimbusJwtDecoder rsaDecoder = null;
    private volatile NimbusJwtDecoder hmacDecoder = null;

    @Override
    public Jwt decode(String token) throws JwtException {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            String algorithm = signedJWT.getHeader().getAlgorithm().getName();
            log.debug("Routing JWT decoding for algorithm: {}", algorithm);

            if ("HS512".equals(algorithm)) {
                return getHmacDecoder().decode(token);
            } else if ("RS256".equals(algorithm)) {
                return getRsaDecoder().decode(token);
            } else {
                throw new JwtException("Unsupported JWT signing algorithm: " + algorithm);
            }
        } catch (Exception e) {
            log.error("JWT decoding failed: {}", e.getMessage());
            throw new JwtException("JWT decode failed: " + e.getMessage());
        }
    }

    private NimbusJwtDecoder getHmacDecoder() {
        if (hmacDecoder == null) {
            synchronized (this) {
                if (hmacDecoder == null) {
                    log.info("Initializing legacy HS512 HMAC JWT Decoder");
                    SecretKeySpec secretKeySpec = new SecretKeySpec(signerKey.getBytes(), "HS512");
                    hmacDecoder = NimbusJwtDecoder.withSecretKey(secretKeySpec)
                            .macAlgorithm(MacAlgorithm.HS512)
                            .build();
                }
            }
        }
        return hmacDecoder;
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
