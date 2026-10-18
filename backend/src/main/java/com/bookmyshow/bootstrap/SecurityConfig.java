package com.bookmyshow.bootstrap;

import com.bookmyshow.common.error.ApiError;
import com.bookmyshow.security.CurrentUser;
import com.bookmyshow.security.Roles;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import tools.jackson.databind.json.JsonMapper;

/**
 * Who can call what. Browse APIs are public. Login APIs are public. Admin APIs need a valid access token, and each
 * admin endpoint checks the role. Everything else needs a token by default, so a new endpoint is never public by
 * mistake.
 */
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JsonMapper jsonMapper) throws Exception {
        // Order matters: the first matching rule wins.
        http.csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.requestMatchers(HttpMethod.GET, "/api/v1/auth/me")
                        .authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/**")
                        .permitAll()
                        // Razorpay calls this. The body signature is checked instead of a login.
                        .requestMatchers(HttpMethod.POST, "/api/v1/payments/razorpay/webhook")
                        .permitAll()
                        .requestMatchers("/api/v1/admin/**")
                        .authenticated()
                        // Public browse APIs, listed one by one. Any other GET (like bookings) needs a token.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/cities/**",
                                "/api/v1/cinemas/**",
                                "/api/v1/screens/**",
                                "/api/v1/movies/**",
                                "/api/v1/shows/**")
                        .permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                        .permitAll()
                        .requestMatchers("/actuator/health/**", "/actuator/info")
                        .permitAll()
                        .requestMatchers("/actuator/**")
                        .hasRole(Roles.ADMIN)
                        .requestMatchers("/error")
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(roleConverter())))
                .exceptionHandling(errors -> errors.authenticationEntryPoint((request, response, ex) ->
                                write(jsonMapper, request, response, HttpStatus.UNAUTHORIZED, "Login required"))
                        .accessDeniedHandler((request, response, ex) -> write(
                                jsonMapper,
                                request,
                                response,
                                HttpStatus.FORBIDDEN,
                                "You do not have access to this resource")));
        return http.build();
    }

    /** Maps the "role" claim to a ROLE_ authority, so hasRole('ADMIN') works. */
    private static Converter<Jwt, AbstractAuthenticationToken> roleConverter() {
        return jwt -> {
            String role = jwt.getClaimAsString(CurrentUser.ROLE_CLAIM);
            List<SimpleGrantedAuthority> authorities =
                    role == null ? List.of() : List.of(new SimpleGrantedAuthority("ROLE_" + role));
            return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
        };
    }

    private static void write(
            JsonMapper jsonMapper,
            HttpServletRequest request,
            HttpServletResponse response,
            HttpStatus status,
            String message)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        jsonMapper.writeValue(response.getOutputStream(), ApiError.of(status, message, request));
    }
}
