package org.example.QuanLyMuaVu.module.shared.config;

import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.example.QuanLyMuaVu.Config.JwtAuthenticationEntryPoint;
import org.example.QuanLyMuaVu.module.identity.config.CustomJwtDecoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Slf4j
public class SecurityConfig {

        // Public endpoints that don't require authentication (for logging purposes)
        private final String[] PUBLIC_ENDPOINTS = {
                        "/api/v1/auth/sign-in",
                        "/api/v1/auth/login",
                        "/api/v1/auth/sign-up",
                        "/api/v1/auth/introspect",
                        "/api/v1/auth/refresh",
                        "/api/v1/auth/forgot-password",
                        "/api/v1/auth/reset-password",
                        "/api/v1/auth/reset-password/validate"
        };

        // Swagger/OpenAPI endpoints that should be accessible without authentication
        private final String[] SWAGGER_ENDPOINTS = {
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",
                        "/swagger-resources/**",
                        "/webjars/**",
                        "/doc.html"
        };

        @Autowired
        private CustomJwtDecoder customJwtDecoder;

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
                log.info("Cau hinh chuoi bo loc bao mat...");

                httpSecurity
                                .cors(Customizer.withDefaults())
                                .csrf(AbstractHttpConfigurer::disable)
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(auth -> auth
                                                // Allow Swagger/OpenAPI endpoints
                                                .requestMatchers(SWAGGER_ENDPOINTS).permitAll()
                                                // Allow preflight requests for CORS
                                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                                                // Public API and authentication endpoints
                                                .requestMatchers("/api/v1/public/**").permitAll()
                                                .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                                                // Public address lookup endpoints (for frontend dropdowns)
                                                .requestMatchers(HttpMethod.GET, "/api/v1/address/**").permitAll()
                                                // Marketplace public catalog endpoints
                                                .requestMatchers(
                                                                HttpMethod.GET,
                                                                "/api/v1/marketplace/products/**",
                                                                "/api/v1/marketplace/farms/**",
                                                                "/api/v1/marketplace/traceability/**")
                                                .permitAll()
                                                .requestMatchers("/api/v1/marketplace/farmer/**").hasRole("FARMER")
                                                .requestMatchers("/api/v1/marketplace/admin/**").hasRole("ADMIN")
                                                // Marketplace buyer capabilities
                                                .requestMatchers(
                                                                "/api/v1/marketplace/cart/**",
                                                                "/api/v1/marketplace/orders/**",
                                                                "/api/v1/marketplace/addresses/**",
                                                                "/api/v1/marketplace/reviews/**",
                                                                "/api/v1/buyer/orders/**")
                                                .hasRole("BUYER")
                                                .requestMatchers("/api/v1/notifications/**")
                                                .hasAnyRole("ADMIN", "BUYER", "FARMER", "EMPLOYEE")
                                                // Admin APIs (system-wide access for admins)
                                                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                                                // Farmer APIs and farm-scoped resources (day-to-day operations)
                                                .requestMatchers("/api/v1/farmer/**").hasRole("FARMER")
                                                .requestMatchers("/api/v1/dashboard/**", "/api/v1/fields/**")
                                                .hasRole("FARMER")
                                                .requestMatchers("/api/v1/farms/**", "/api/v1/plots/**",
                                                                "/api/v1/crops/**",
                                                                "/api/v1/seasons/**")
                                                .hasRole("FARMER")
                                                .requestMatchers("/api/v1/inventory/**").hasRole("FARMER")
                                                .requestMatchers("/api/v1/product-warehouses/**").hasRole("FARMER")
                                                // Buyer APIs
                                                .requestMatchers("/api/v1/buyer/**").hasRole("BUYER")
                                                // Employee APIs
                                                .requestMatchers("/api/v1/employee/**").hasRole("EMPLOYEE")
                                                .requestMatchers(HttpMethod.POST, "/api/v1/firebase/chat-token")
                                                .authenticated()
                                                .requestMatchers("/api/v1/preferences/**")
                                                .hasAnyRole("ADMIN", "BUYER", "FARMER", "EMPLOYEE")
                                                // Shared user APIs (ADMIN + BUYER + FARMER + EMPLOYEE)
                                                .requestMatchers("/api/v1/user/**")
                                                .hasAnyRole("ADMIN", "BUYER", "FARMER", "EMPLOYEE")
                                                // Mixed shared features
                                                .requestMatchers("/api/v1/reports/**", "/api/v1/ai/**")
                                                .hasRole("BUYER")
                                                .requestMatchers("/api/v1/documents/**").hasRole("FARMER")
                                                // All other requests require authentication
                                                .anyRequest().authenticated())
                                .oauth2ResourceServer(oauth2 -> oauth2
                                                .jwt(jwtConfigurer -> jwtConfigurer
                                                                .decoder(customJwtDecoder)
                                                                .jwtAuthenticationConverter(
                                                                                jwtAuthenticationConverter()))
                                                .authenticationEntryPoint(new JwtAuthenticationEntryPoint()));

                log.info("Cau hinh bao mat da hoan tat thanh cong");
                log.info("Public endpoints: {}", Arrays.toString(PUBLIC_ENDPOINTS));
                log.info("Swagger endpoints: {}", Arrays.toString(SWAGGER_ENDPOINTS));

                return httpSecurity.build();
        }

        @Bean
        JwtAuthenticationConverter jwtAuthenticationConverter() {
                log.info("Cau hinh Bo chuyen doi xac thuc JWT...");

                JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
                jwtGrantedAuthoritiesConverter.setAuthorityPrefix("");
                jwtGrantedAuthoritiesConverter.setAuthoritiesClaimName("scope");

                JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
                jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter);

                log.info("Bo chuyen doi xac thuc JWT da duoc cau hinh thanh cong");
                return jwtAuthenticationConverter;
        }

        @Bean
        PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder(10);
        }
}
