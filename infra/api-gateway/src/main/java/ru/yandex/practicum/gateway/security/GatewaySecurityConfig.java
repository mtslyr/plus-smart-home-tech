package ru.yandex.practicum.gateway.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;

@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfig {

    @Bean
    SecurityWebFilterChain apiSecurityWebFilterChain(ServerHttpSecurity http) {
        return http
                .httpBasic(Customizer.withDefaults())
                .authorizeExchange(exchange ->
                        exchange
                                .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                                .pathMatchers(HttpMethod.GET,
                                        "/api/categories/**",
                                        "/api/products/**").permitAll()
                                .pathMatchers(HttpMethod.GET,
                                        "/api/inventory/**",
                                        "/api/orders/{id}",
                                        "/api/orders/by-email").hasRole("USER")
                                .pathMatchers(HttpMethod.GET, "/api/orders").hasRole("ADMIN")
                                .pathMatchers(HttpMethod.POST, "/api/orders").hasRole("USER")
                                .pathMatchers(HttpMethod.POST,
                                        "/api/categories",
                                        "/api/inventory",
                                        "/api/products").hasRole("ADMIN")
                                .pathMatchers(HttpMethod.PUT, "/api/inventory").hasRole("ADMIN")
                                .pathMatchers(HttpMethod.PATCH, "/api/products/{id}").hasRole("ADMIN")
                                .anyExchange().denyAll()

                )
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .build();
    }

    @Bean
    MapReactiveUserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        UserDetails ivan = User.withUsername("ivan")
                .password(passwordEncoder.encode("ivan"))
                .roles("USER")
                .build();

        UserDetails anna = User.withUsername("anna")
                .password(passwordEncoder.encode("anna"))
                .roles("ADMIN", "USER")
                .build();

        return new MapReactiveUserDetailsService(ivan, anna);
    }

    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
}
