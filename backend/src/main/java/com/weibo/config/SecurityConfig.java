package com.weibo.config;

import com.weibo.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 配置
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // 公开接口 - 无需鉴权 (注意：context-path 是 /api/v1，所以这里不需要前缀)
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers("/users/register").permitAll()
                .requestMatchers("/users/login").permitAll()
                .requestMatchers(HttpMethod.GET, "/users/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/weibos/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/comments/**").permitAll()
                // AI 接口 - 无需鉴权
                .requestMatchers("/ai/**").permitAll()
                // 上传接口 - 无需鉴权
                .requestMatchers("/upload/**").permitAll()
                // 静态资源 - 无需鉴权
                .requestMatchers("/uploads/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                // 其他接口需要鉴权
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}
