package neyan.tech.baraka_backend.common.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final JwtAccessDeniedHandler accessDeniedHandler;

    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/auth/**",
            "/api-docs/**",
            "/api-docs.yaml",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/actuator/health",
            "/actuator/info"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        // Shop management - MERCHANT and ADMIN
                        .requestMatchers(HttpMethod.POST, "/api/v1/shops/**").hasAnyRole("MERCHANT", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/shops/**").hasAnyRole("MERCHANT", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/shops/**").hasAnyRole("MERCHANT", "ADMIN")
                        // Basket management - MERCHANT and ADMIN
                        .requestMatchers(HttpMethod.POST, "/api/v1/baskets/**").hasAnyRole("MERCHANT", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/baskets/**").hasAnyRole("MERCHANT", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/baskets/**").hasAnyRole("MERCHANT", "ADMIN")
                        // Orders - CUSTOMER can create/cancel their own
                        .requestMatchers(HttpMethod.POST, "/api/v1/orders/**").hasAnyRole("CUSTOMER", "ADMIN")
                        // Reviews - CUSTOMER can post
                        .requestMatchers(HttpMethod.POST, "/api/v1/reviews/**").hasAnyRole("CUSTOMER", "ADMIN")
                        // Favorites - CUSTOMER can manage
                        .requestMatchers("/api/v1/favorites/**").hasAnyRole("CUSTOMER", "ADMIN")
                        // Admin endpoints
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        // All other requests need authentication
                        .anyRequest().authenticated())
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
