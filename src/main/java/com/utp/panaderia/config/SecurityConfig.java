package com.utp.panaderia.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;

import com.utp.panaderia.model.Rol;
import com.utp.panaderia.security.VerificacionDosPasosFilter;

import jakarta.servlet.http.HttpSession;

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
                .requestMatchers("/css/**", "/js/**", "/img/**", "/uploads/**").permitAll()
                .requestMatchers("/admin/**").hasRole(Rol.ADMINISTRADOR)
                // los mensajes los escriben los clientes; el administrador responde desde /admin/mensajes
                .requestMatchers("/mensajes", "/mensajes/**").hasRole(Rol.CLIENTE)
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login")
                .usernameParameter("email")
                .successHandler(pedirSegundoPaso())
                .failureUrl("/login?error")
                .permitAll())
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?salir")
                .permitAll())
            .exceptionHandling(ex -> ex.accessDeniedPage("/acceso-denegado"))
            // no deja usar la cuenta hasta ingresar el codigo de 6 digitos
            .addFilterAfter(new VerificacionDosPasosFilter(), AuthorizationFilter.class);

        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Contraseña correcta: falta el segundo paso (codigo de la app autenticadora).
    // Se guarda a donde ira despues: el administrador al panel, el cliente a la pagina
    // que estaba intentando abrir (ej. confirmar pedido) o al inicio.
    private AuthenticationSuccessHandler pedirSegundoPaso() {
        RequestCache paginaPendiente = new HttpSessionRequestCache();

        return (request, response, authentication) -> {
            boolean esAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_" + Rol.ADMINISTRADOR));
            SavedRequest guardada = paginaPendiente.getRequest(request, response);
            paginaPendiente.removeRequest(request, response);

            String destino = "/";
            if (esAdmin) {
                destino = "/admin/productos";
            } else if (guardada != null) {
                destino = guardada.getRedirectUrl();
            }

            HttpSession sesion = request.getSession();
            sesion.setAttribute(VerificacionDosPasosFilter.PENDIENTE, true);
            sesion.setAttribute(VerificacionDosPasosFilter.DESTINO, destino);
            sesion.removeAttribute(VerificacionDosPasosFilter.INTENTOS);
            response.sendRedirect(request.getContextPath() + "/verificacion");
        };
    }
}
