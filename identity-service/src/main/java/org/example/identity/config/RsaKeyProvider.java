package org.example.identity.config;

import com.nimbusds.jose.jwk.RSAKey;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.UUID;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * Loads RSA key pair from PEM files and exposes them for JWT signing / JWKS.
 *
 * Configuration properties:
 *   jwt.rsa.private-key-location  (default: classpath:keys/rsa-private.pem)
 *   jwt.rsa.public-key-location   (default: classpath:keys/rsa-public.pem)
 */
@Component
@Slf4j
@Getter
public class RsaKeyProvider {

    private final RSAPublicKey publicKey;
    private final RSAPrivateKey privateKey;
    private final RSAKey rsaJwk;

    public RsaKeyProvider(
            @Value("${jwt.rsa.private-key-location:classpath:keys/rsa-private.pem}") Resource privateKeyResource,
            @Value("${jwt.rsa.public-key-location:classpath:keys/rsa-public.pem}") Resource publicKeyResource) {
        try {
            this.privateKey = loadPrivateKey(privateKeyResource);
            this.publicKey = loadPublicKey(publicKeyResource);

            // Build a nimbus RSAKey (contains both public + private for signing)
            this.rsaJwk = new RSAKey.Builder(this.publicKey)
                    .privateKey(this.privateKey)
                    .keyID("acm-identity-" + UUID.randomUUID().toString().substring(0, 8))
                    .build();

            log.info("RSA key pair loaded successfully — kid={}", rsaJwk.getKeyID());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load RSA key pair for JWT signing", e);
        }
    }

    /**
     * Returns the public-only JWK for the JWKS endpoint.
     */
    public RSAKey getPublicJwk() {
        return rsaJwk.toPublicJWK();
    }

    // ---- helpers ----

    private RSAPrivateKey loadPrivateKey(Resource resource) throws Exception {
        String pem = readPem(resource);
        pem = pem.replace("-----BEGIN PRIVATE KEY-----", "")
                 .replace("-----END PRIVATE KEY-----", "")
                 .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                 .replace("-----END RSA PRIVATE KEY-----", "")
                 .replaceAll("\\s", "");

        byte[] decoded = Base64.getDecoder().decode(pem);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
        return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(spec);
    }

    private RSAPublicKey loadPublicKey(Resource resource) throws Exception {
        String pem = readPem(resource);
        pem = pem.replace("-----BEGIN PUBLIC KEY-----", "")
                 .replace("-----END PUBLIC KEY-----", "")
                 .replaceAll("\\s", "");

        byte[] decoded = Base64.getDecoder().decode(pem);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
        return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
    }

    private String readPem(Resource resource) throws IOException {
        try (InputStream is = resource.getInputStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
