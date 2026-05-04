package org.example.QuanLyMuaVu.Util;

import jakarta.servlet.http.HttpServletRequest;
import org.example.QuanLyMuaVu.Config.FarmSecurityProperties;

public final class RequestUtils {

    private RequestUtils() {
    }

    public static String resolveClientIp(HttpServletRequest request, FarmSecurityProperties properties) {
        if (request == null) {
            return null;
        }

        String remoteAddr = request.getRemoteAddr();
        if (properties != null && properties.isTrustProxyHeaders() && properties.isTrustedProxy(remoteAddr)) {
            String forwardedFor = request.getHeader("X-Forwarded-For");
            if (forwardedFor != null && !forwardedFor.isBlank()) {
                String[] parts = forwardedFor.split(",");
                if (parts.length > 0 && !parts[0].isBlank()) {
                    return parts[0].trim();
                }
            }
            String realIp = request.getHeader("X-Real-IP");
            if (realIp != null && !realIp.isBlank()) {
                return realIp.trim();
            }
        }

        return remoteAddr;
    }

    public static String resolveUserAgent(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        return request.getHeader("User-Agent");
    }
}
