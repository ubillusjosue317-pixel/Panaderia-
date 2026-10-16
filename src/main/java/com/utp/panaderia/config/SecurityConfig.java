package com.utp.panaderia.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

import com.utp.panaderia.model.Rol;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/explorar", "/catalogo", "/login", "/registro", "/error").permitAll()
                // carrito y favoritos se pueden usar sin cuenta, solo confirmar el pedido pide login
                .requestMatchers("/pedidos", "/carrito/**", "/favoritos", "/favoritos/**").permitAll()
                .requestMatchers("/css/**", "/js/**", "/img/**").permitAll()
                .requestMatchers("/admin/**").hasRole(Rol.ADMINISTRADOR)
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login")
                .usernameParameter("email")
                .successHandler(redirigirSegunRol())
                .failureUrl("/login?error")
                .permitAll())
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?salir")
                .permitAll())
            .exceptionHandling(ex -> ex.accessDeniedPage("/acceso-denegado"));

        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // El administrador entra directo al panel.
    // El cliente vuelve a la pagina que estaba intentando abrir (ej. confirmar pedido) o al inicio.
    private AuthenticationSuccessHandler redirigirSegunRol() {
        SavedRequestAwareAuthenticationSuccessHandler cliente = new SavedRequestAwareAuthenticationSuccessHandler();
        cliente.setDefaultTargetUrl("/");

        return (request, response, authentication) -> {
            boolean esAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_" + Rol.ADMINISTRADOR));
            if (esAdmin) {
                response.sendRedirect(request.getContextPath() + "/admin/productos");
            } else {
                cliente.onAuthenticationSuccess(request, response, authentication);
            }
        };
    }
}
