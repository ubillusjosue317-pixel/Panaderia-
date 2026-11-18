package com.utp.panaderia.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;

import com.utp.panaderia.model.Rol;
import com.utp.panaderia.security.BloqueoIntentos;
import com.utp.panaderia.security.VerificacionDosPasosFilter;

import jakarta.servlet.http.HttpSession;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // Politica de contenido: solo recursos propios y Google Fonts (Bootstrap ya se sirve desde el sitio).
    // 'unsafe-inline' se mantiene porque las vistas usan scripts y estilos dentro de la pagina.
    private static final String CSP = "default-src 'self'; "
            + "script-src 'self' 'unsafe-inline'; "
            + "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; "
            + "font-src 'self' https://fonts.gstatic.com; "
            + "img-src 'self' data: blob:; "
            + "connect-src 'self'; object-src 'none'; base-uri 'self'; form-action 'self'; frame-ancestors 'none'";

    @Value("${app.https.forzar:false}")
    private boolean forzarHttps;

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, BloqueoIntentos bloqueo) throws Exception {
        if (forzarHttps) {
            http.requiresChannel(canal -> canal.anyRequest().requiresSecure());
        }
        http
            .headers(h -> h
                .contentSecurityPolicy(csp -> csp.policyDirectives(CSP))
                .frameOptions(frame -> frame.deny())
                .referrerPolicy(ref -> ref.policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000))
                .addHeaderWriter(new StaticHeadersWriter("Permissions-Policy",
                        "camera=(), microphone=(), geolocation=()")))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/explorar", "/catalogo", "/login", "/registro", "/registro/verificacion", "/error").permitAll()
                // carrito y favoritos se pueden usar sin cuenta, solo confirmar el pedido pide login
                .requestMatchers("/pedidos", "/carrito/**", "/favoritos", "/favoritos/**").permitAll()
                .requestMatchers("/css/**", "/js/**", "/img/**", "/vendor/**", "/uploads/**").permitAll()
                .requestMatchers("/admin/**").hasRole(Rol.ADMINISTRADOR)
                // los mensajes los escriben los clientes; el administrador responde desde /admin/mensajes
                .requestMatchers("/mensajes", "/mensajes/**").hasRole(Rol.CLIENTE)
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login")
                .usernameParameter("email")
                .successHandler(pedirSegundoPaso())
                // si la cuenta esta bloqueada se avisa con otro mensaje
                .failureHandler((request, response, excepcion) -> {
                    String email = request.getParameter("email");
                    String destino = bloqueo.estaBloqueado(email) ? "/login?cuentaBloqueada" : "/login?error";
                    response.sendRedirect(request.getContextPath() + destino);
                })
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

    // Los archivos que el navegador pide solo (sw.js, favicon.ico, mapas...) no son una pagina
    // a la que el usuario queria ir: no se usan como destino despues de iniciar sesion.
    private boolean esPagina(String url) {
        try {
            String ruta = java.net.URI.create(url).getPath();
            return ruta != null && !ruta.matches(".*\\.[A-Za-z0-9]{1,5}$");
        } catch (IllegalArgumentException e) {
            return false;
        }
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
            } else if (guardada != null && esPagina(guardada.getRedirectUrl())) {
                destino = guardada.getRedirectUrl();
            }

            HttpSession sesion = request.getSession();
            sesion.setAttribute(VerificacionDosPasosFilter.PENDIENTE, true);
            sesion.setAttribute(VerificacionDosPasosFilter.DESTINO, destino);
            sesion.setAttribute(VerificacionDosPasosFilter.INICIO, System.currentTimeMillis());
            sesion.removeAttribute(VerificacionDosPasosFilter.INTENTOS);
            response.sendRedirect(request.getContextPath() + "/verificacion");
        };
    }
}
