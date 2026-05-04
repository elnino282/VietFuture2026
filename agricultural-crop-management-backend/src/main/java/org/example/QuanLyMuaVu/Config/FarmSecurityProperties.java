package org.example.QuanLyMuaVu.Config;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for Farm module security.
 * Maps to the farm.security section in application.yaml
 */
@Configuration
@ConfigurationProperties(prefix = "farm.security")
@Data
public class FarmSecurityProperties {

    /**
     * Whether to trust X-Forwarded-For header for IP extraction.
     * Set to true only in production behind trusted load balancer/proxy.
     * 
     * Security Note: If enabled without proper proxy configuration,
     * clients can spoof their IP address bypassing audit trails.
     */
    private boolean trustProxyHeaders = false;

    /**
     * List of trusted proxy IP addresses.
     * Only requests originating from these IPs will have X-Forwarded-For header
     * trusted.
     * 
     * Leave empty to trust all proxies (not recommended for production).
     * 
     * Supports:
     * - Individual IPs: "10.0.0.1"
     * - CIDR notation: "172.16.0.0/12" (for future implementation)
     */
    private List<String> trustedProxyIps = new ArrayList<>();

    /**
     * Check if a given remote address is in the trusted proxy list.
     * 
     * @param remoteAddr The remote address from the request
     * @return true if remoteAddr is trusted, false otherwise
     */
    public boolean isTrustedProxy(String remoteAddr) {
        if (trustedProxyIps == null || trustedProxyIps.isEmpty()) {
            // Empty list means trust all proxies (when trustProxyHeaders = true)
            return true;
        }

        // Simple IP match (CIDR support can be added later if needed)
        return trustedProxyIps.contains(remoteAddr);
    }
}
