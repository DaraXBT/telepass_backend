package com.example.tb.configuration;

import java.util.Arrays;
import java.util.Collections;

import com.example.tb.authentication.service.admin.AdminServiceImpl;
import com.example.tb.jwt.JwtAuthEntryPoint;
import com.example.tb.jwt.JwtTokenFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@Configuration
@EnableWebSecurity
public class DataBaseSecurity {
    private final AdminServiceImpl adminServiceImpl;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenFilter jwtTokenFilter;
    private final JwtAuthEntryPoint authEntryPoint;

    public DataBaseSecurity(AdminServiceImpl adminServiceImpl, PasswordEncoder passwordEncoder, JwtTokenFilter jwtTokenFilter, JwtAuthEntryPoint authEntryPoint) {
        this.adminServiceImpl = adminServiceImpl;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenFilter = jwtTokenFilter;
        this.authEntryPoint = authEntryPoint;
    }
    @Bean
    DaoAuthenticationProvider authenticationProvider(){
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(adminServiceImpl);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors();
        http.csrf()
                .disable()
                .authorizeHttpRequests(request -> request
                        .requestMatchers(
                                "/api/v1/admin/**",
                                "/api/v1/images/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger/ui.html").permitAll()
                        .requestMatchers(
                                "/api/v1/events/**",
                                "/api/v1/audiences/**"
                        ).authenticated()
                        .anyRequest().authenticated()
                )
                .exceptionHandling()
                .authenticationEntryPoint(authEntryPoint)
                .and()
                .addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

        @Bean
        public CorsFilter corsFilter() {
                final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                final CorsConfiguration config = new CorsConfiguration();
                config.setAllowCredentials(true);
                config.setAllowedOriginPatterns(Collections.singletonList("*"));
                config.setAllowedHeaders(
                                Arrays.asList(
                                                "X-Requested-With",
                                                "Origin",
                                                "Content-Type",
                                                "Accept",
                                                "Authorization",
                                                "Access-Control-Allow-Credentials",
                                                "Access-Control-Allow-Headers",
                                                "Access-Control-Allow-Methods",
                                                "Access-Control-Allow-Origin",
                                                "Access-Control-Expose-Headers",
                                                "Access-Control-Max-Age",
                                                "Access-Control-Request-Headers",
                                                "Access-Control-Request-Method",
                                                "Age",
                                                "Allow",
                                                "Alternates",
                                                "Content-Range",
                                                "Content-Disposition",
                                                "Content-Description"));
                config.setAllowedMethods(
                                Arrays.asList("GET", "POST", "PUT", "OPTIONS", "DELETE", "PATCH"));
                source.registerCorsConfiguration("/**", config);
                return new CorsFilter(source);
        }

}