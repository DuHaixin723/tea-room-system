package com.tea.management.config;

import com.tea.management.service.JwtTokenService;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
/**
 * SecurityConfig 配置类，用来声明系统运行时需要的框架配置。
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@EnableConfigurationProperties({SecurityProperties.class, AiAssistantProperties.class})
public class SecurityConfig {

    private final JwtTokenService jwtTokenService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(jwtTokenService);

        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {
                })
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.ASYNC).permitAll()
                        .requestMatchers("/error", "/api/auth/**", "/api/health/**", "/api/files/public-images", "/api/system-configs/basic-settings", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/uploads/**").permitAll()
                        .requestMatchers("/ws-consultation/**").hasAnyRole("USER", "STAFF", "ADMIN")
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> writeJsonResponse(
                                response,
                                HttpServletResponse.SC_UNAUTHORIZED,
                                "{\"success\":false,\"message\":\"未授权访问\",\"data\":null}"
                        ))
                        .accessDeniedHandler((request, response, accessDeniedException) -> writeJsonResponse(
                                response,
                                HttpServletResponse.SC_FORBIDDEN,
                                "{\"success\":false,\"message\":\"禁止访问\",\"data\":null}"
                        )))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private void writeJsonResponse(HttpServletResponse response, int status, String body) throws java.io.IOException {
        if (response.isCommitted()) {
            return;
        }
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(body);
    }
}
