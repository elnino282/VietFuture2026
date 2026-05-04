package org.example.QuanLyMuaVu.module.identity.config;

import com.nimbusds.jose.JOSEException;
import java.text.ParseException;
import java.util.Objects;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.example.QuanLyMuaVu.module.identity.dto.request.IntrospectRequest;
import org.example.QuanLyMuaVu.module.identity.service.AuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CustomJwtDecoder implements JwtDecoder {
    @Value("${jwt.signerKey}")
    private String signerKey;

    @Autowired
    @Lazy
    private AuthenticationService authenticationService;

    private NimbusJwtDecoder nimbusJwtDecoder = null;

    @Override
    public Jwt decode(String token) throws JwtException {
        log.debug("Dang co gang giai ma JWT token: {}", token.substring(0, Math.min(20, token.length())) + "...");

        try {
            // First, introspect the token to check if it's valid
            var response = authenticationService.introspect(
                    IntrospectRequest.builder().token(token).build());

            if (!response.isValid()) {
                log.warn("Kiem tra token khong thanh cong token khong hop le");
                throw new JwtException("Token invalid");
            }

            log.debug("Kiem tra token thanh cong tien hanh giai ma JWT");

        } catch (JOSEException | ParseException e) {
            log.error("Loi trong qua trinh tu kiem tra token: {}", e.getMessage(), e);
            throw new JwtException("Xac thuc token that bai: " + e.getMessage());
        }

        // Initialize JWT decoder if not already done
        if (Objects.isNull(nimbusJwtDecoder)) {
            log.debug("Khoi tao bo giai ma Nimbus JWT bang thuat toan HS512");
            try {
                SecretKeySpec secretKeySpec = new SecretKeySpec(signerKey.getBytes(), "HS512");
                nimbusJwtDecoder = NimbusJwtDecoder.withSecretKey(secretKeySpec)
                        .macAlgorithm(MacAlgorithm.HS512)
                        .build();
                log.debug("Bo giai ma Nimbus JWT da duoc khoi tao thanh cong");
            } catch (Exception e) {
                log.error("Khong the khoi tao bo giai ma JWT: {}", e.getMessage(), e);
                throw new JwtException("Khoi tao bo giai ma JWT khong thanh cong: " + e.getMessage());
            }
        }

        try {
            Jwt decodedJwt = nimbusJwtDecoder.decode(token);
            log.debug("JWT token da duoc giai ma thanh cong cho chu the: {}", decodedJwt.getSubject());
            return decodedJwt;
        } catch (Exception e) {
            log.error("Khong giai ma duoc JWT token: {}", e.getMessage(), e);
            throw new JwtException("Giai ma JWT khong thanh cong: " + e.getMessage());
        }
    }
}
