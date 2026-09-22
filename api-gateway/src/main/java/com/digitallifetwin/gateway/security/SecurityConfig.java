package com.digitallifetwin.gateway.security;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.authentication.ServerAuthenticationEntryPointFailureHandler;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.security.web.server.header.ReferrerPolicyServerHttpHeadersWriter;
import org.springframework.security.web.server.header.XFrameOptionsServerHttpHeadersWriter;
import org.springframework.web.cors.reactive.CorsConfigurationSource;

@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtReactiveAuthenticationManager authenticationManager;
    private final BearerTokenServerAuthenticationConverter authenticationConverter;
    private final JsonAuthenticationEntryPoint authenticationEntryPoint;
    private final JsonAccessDeniedHandler accessDeniedHandler;
    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        AuthenticationWebFilter jwtFilter = new AuthenticationWebFilter(authenticationManager);
        jwtFilter.setServerAuthenticationConverter(authenticationConverter);
        jwtFilter.setAuthenticationFailureHandler(
                new ServerAuthenticationEntryPointFailureHandler(authenticationEntryPoint));
        jwtFilter.setSecurityContextRepository(NoOpServerSecurityContextRepository.getInstance());

        return http
                // JWT Bearer in Authorization (stateless). CSRF targets cookie sessions, not this API.
                .csrf(csrf -> csrf.disable()) // NOSONAR java:S4502
                .headers(headers -> headers
                        .contentTypeOptions(Customizer.withDefaults())
                        .frameOptions(frame -> frame.mode(XFrameOptionsServerHttpHeadersWriter.Mode.DENY))
                        .referrerPolicy(referrer -> referrer.policy(
                                ReferrerPolicyServerHttpHeadersWriter.ReferrerPolicy.NO_REFERRER))
                        .hsts(hsts -> hsts.includeSubdomains(true).maxAge(Duration.ofDays(365)))
                        .contentSecurityPolicy(csp -> csp.policyDirectives("frame-ancestors 'none'")))
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(HttpMethod.OPTIONS).permitAll()
                        .pathMatchers(
                                "/api/auth/register",
                                "/api/auth/register/send-code",
                                "/api/auth/login",
                                "/api/auth/google",
                                "/api/auth/refresh",
                                "/api/auth/forgot-password",
                                "/api/auth/reset-password",
                                "/api/auth/contact"
                        ).permitAll()
                        .pathMatchers(
                                "/actuator/health",
                                "/actuator/health/**",
                                "/actuator/info"
                        ).permitAll()
                        .anyExchange().authenticated())
                .addFilterAt(jwtFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }
}
