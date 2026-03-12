package com.petfinder.chat.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Deshabilitamos CSRF para APIs e integraciones desde otros clientes
                .authorizeHttpRequests(auth -> auth
                        // Permitimos tráfico a websockets de forma inicial, luego se pueden inyectar
                        // interceptores STOMP
                        .requestMatchers("/ws/**").permitAll()
                        // Permitimos GraphiQL para pruebas, y la API en general porque la validación
                        // puede ser interna
                        // pero lo estricto es autenticar todas estas rutas:
                        // .requestMatchers("/graphql").authenticated()
                        .requestMatchers("/graphql", "/graphiql").permitAll()
                        .anyRequest().authenticated())
                // Habilitamos Resource Server esperando JWT (requiere especificar la URL de
                // Google en app.yml)
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {
                }));

        return http.build();
    }
}
