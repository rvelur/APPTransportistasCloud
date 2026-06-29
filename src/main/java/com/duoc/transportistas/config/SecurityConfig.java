package com.duoc.transportistas.config;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Deshabilitado para poder usar APIs REST libremente
            .authorizeHttpRequests(auth -> auth
                // 1. Rol Descargador: Solo puede usar el endpoint de descargar
                .requestMatchers(HttpMethod.GET, "/api/guias/{id}/descargar").hasRole("DESCARGADOR")
                
                // 2. Rol Admin: Puede hacer todo el resto de las operaciones
                .requestMatchers(HttpMethod.POST, "/api/guias", "/api/guias/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/guias/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/guias/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/guias/buscar").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/guias").hasRole("ADMIN")
                
                // Cualquier otra ruta requiere estar autenticado
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
            
        return http.build();
    }

    // Mapeador para leer los roles (extension_roles o roles) que Azure inyecta en el Token JWT
    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            // Azure AD B2C suele mandar los roles en un claim llamado "roles" o "extension_roles"
            List<String> roles = jwt.getClaimAsStringList("role");
            if (roles == null) {
                roles = jwt.getClaimAsStringList("extension_role");
            }
            if (roles == null) return Collections.emptyList();
            
            return roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                    .collect(Collectors.toList());
        });
        return converter;
    }
}