package com.ironcore.ironcorebackend.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CORS with our configuration below
            .cors(Customizer.withDefaults())

            // For JSON API with custom auth we keep CSRF disabled.
            // If you later rely on cookies/session-based auth, enable CSRF.
            .csrf(csrf -> csrf.disable())

            // --- OWASP: configure which endpoints are public ---
            .authorizeHttpRequests(auth -> auth
                // Login / register / password reset can be public
                .requestMatchers(
                        "/api/auth/login",
                        "/api/auth/register",
                        "/api/auth/forgot-password",
                        "/error"
                ).permitAll()
                // For now, allow everything else (you can tighten later)
                .anyRequest().permitAll()
            )

            // Session handling — IF_REQUIRED keeps current behavior
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            )

            // --- OWASP: security headers (defense-in-depth) ---
            .headers(headers -> headers
                // Prevent clickjacking
                .frameOptions(frame -> frame.deny())
                // Basic Content Security Policy (tweak when you deploy)
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives("default-src 'self'; frame-ancestors 'none'; form-action 'self';")
                )
            );

        return http.build();
    }

    // CORS Configuration Bean - NOW restricted to your frontend origin
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Only allow your React dev origin (add prod domain later)
        configuration.setAllowedOrigins(List.of("http://localhost:3000"));

        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "OPTIONS"
        ));
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // Needed because you use credentials: "include" in fetch
        configuration.setAllowCredentials(true);

        configuration.setMaxAge(3600L); // Cache preflight requests for 1 hour

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    // Password encoder bean (good! this matches OWASP recommendations)
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
