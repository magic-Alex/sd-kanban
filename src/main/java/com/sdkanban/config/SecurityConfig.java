package com.sdkanban.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdkanban.common.ApiResponse;
import com.sdkanban.user.entity.User;
import com.sdkanban.user.repository.UserRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        BearerTokenAuthenticationFilter bearerTokenAuthenticationFilter,
        AuthenticationEntryPoint authenticationEntryPoint,
        AccessDeniedHandler accessDeniedHandler,
        CorsConfigurationSource corsConfigurationSource
    ) throws Exception {
        return http
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler))
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll())
            .addFilterBefore(bearerTokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
        @Value("${app.cors.allowed-origins}") String allowedOrigins
    ) {
        List<String> originPatterns = Arrays.stream(allowedOrigins.split(","))
            .map(String::trim)
            .filter(origin -> !origin.isBlank())
            .toList();

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(originPatterns);
        configuration.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    @Bean
    BearerTokenAuthenticationFilter bearerTokenAuthenticationFilter(
        JwtService jwtService,
        ObjectProvider<UserRepository> userRepositoryProvider
    ) {
        return new BearerTokenAuthenticationFilter(jwtService, userRepositoryProvider);
    }

    @Bean
    UserDetailsService userDetailsService() {
        return username -> {
            throw new UsernameNotFoundException(username);
        };
    }

    @Bean
    AuthenticationEntryPoint authenticationEntryPoint(ObjectMapper objectMapper) {
        return (request, response, exception) -> writeError(
            response,
            HttpServletResponse.SC_UNAUTHORIZED,
            ApiResponse.error("UNAUTHORIZED", "Authentication required"),
            objectMapper
        );
    }

    @Bean
    AccessDeniedHandler accessDeniedHandler(ObjectMapper objectMapper) {
        return (request, response, exception) -> writeError(
            response,
            HttpServletResponse.SC_FORBIDDEN,
            ApiResponse.error("FORBIDDEN", "Access denied"),
            objectMapper
        );
    }

    private static void writeError(
        HttpServletResponse response,
        int status,
        ApiResponse<Void> body,
        ObjectMapper objectMapper
    ) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }

    static class BearerTokenAuthenticationFilter extends OncePerRequestFilter {
        private final JwtService jwtService;
        private final ObjectProvider<UserRepository> userRepositoryProvider;

        BearerTokenAuthenticationFilter(JwtService jwtService, ObjectProvider<UserRepository> userRepositoryProvider) {
            this.jwtService = jwtService;
            this.userRepositoryProvider = userRepositoryProvider;
        }

        @Override
        protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
        ) throws ServletException, IOException {
            String token = bearerToken(request);
            if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                authenticate(token);
            }

            filterChain.doFilter(request, response);
        }

        private void authenticate(String token) {
            try {
                String account = jwtService.parseAccount(token);
                UserRepository userRepository = userRepositoryProvider.getIfAvailable();
                if (userRepository == null) {
                    return;
                }

                userRepository.findByAccount(account)
                    .filter(user -> "ACTIVE".equals(user.getStatus()))
                    .ifPresent(user -> SecurityContextHolder.getContext().setAuthentication(authentication(user)));
            } catch (JwtException | IllegalArgumentException ignored) {
                SecurityContextHolder.clearContext();
            }
        }

        private UsernamePasswordAuthenticationToken authentication(User user) {
            return new UsernamePasswordAuthenticationToken(user, null, List.of());
        }

        private String bearerToken(HttpServletRequest request) {
            String authorization = request.getHeader("Authorization");
            if (authorization == null || !authorization.startsWith("Bearer ")) {
                return null;
            }
            return authorization.substring(7);
        }
    }
}
