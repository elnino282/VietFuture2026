package org.example.identity.config;

import com.nimbusds.jose.JOSEException;
import java.text.ParseException;
import lombok.extern.slf4j.Slf4j;
import org.example.identity.dto.request.IntrospectRequest;
import org.example.identity.service.AuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

/**
 * Custom JWT decoder that first introspects the token (blacklist check),
 * then verifies the RS256 signature using the local RSA public key.
 */
@Component
@Slf4j
public class CustomJwtDecoder implements JwtDecoder {

    private final RsaKeyProvider rsaKeyProvider;

    @Autowired
    @Lazy
    private AuthenticationService authenticationService;

    private NimbusJwtDecoder nimbusJwtDecoder = null;

    public CustomJwtDecoder(RsaKeyProvider rsaKeyProvider) {
        this.rsaKeyProvider = rsaKeyProvider;
    }

    @Override
    public Jwt decode(String token) throws JwtException {
        log.debug("Decoding JWT token (RS256)...");

        // 1. Introspect — check blacklist
        try {
            var response = authenticationService.introspect(
                    IntrospectRequest.builder().token(token).build());

            if (!response.isValid()) {
                log.warn("Token introspection failed — token is invalid/blacklisted");
                throw new JwtException("Token invalid");
            }
        } catch (JOSEException | ParseException e) {
            log.error("Token introspection error: {}", e.getMessage(), e);
            throw new JwtException("Token verification failed: " + e.getMessage());
        }

        // 2. Verify RS256 signature with local public key
        if (nimbusJwtDecoder == null) {
            log.info("Initializing NimbusJwtDecoder with RSA public key (RS256)");
            nimbusJwtDecoder = NimbusJwtDecoder
                    .withPublicKey(rsaKeyProvider.getPublicKey())
                    .signatureAlgorithm(SignatureAlgorithm.RS256)
                    .build();
        }

        try {
            Jwt decodedJwt = nimbusJwtDecoder.decode(token);
            log.debug("JWT decoded successfully — subject: {}", decodedJwt.getSubject());
            return decodedJwt;
        } catch (Exception e) {
            log.error("JWT decode failed: {}", e.getMessage(), e);
            throw new JwtException("JWT decode failed: " + e.getMessage());
        }
    }
}
