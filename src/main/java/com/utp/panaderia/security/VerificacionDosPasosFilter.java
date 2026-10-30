package com.utp.panaderia.security;

import java.io.IOException;

import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

// Despues de poner bien la contraseña la sesion queda "pendiente" hasta que el usuario
// escriba el codigo de su app autenticadora. Mientras tanto solo puede abrir /verificacion.
public class VerificacionDosPasosFilter extends OncePerRequestFilter {

    // atributos que se guardan en la sesion
    public static final String PENDIENTE = "dosPasos.pendiente";
    public static final String DESTINO = "dosPasos.destino";
    public static final String SECRETO_NUEVO = "dosPasos.secretoNuevo";
    public static final String INTENTOS = "dosPasos.intentos";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        HttpSession sesion = request.getSession(false);
        if (sesion != null && sesion.getAttribute(PENDIENTE) != null && !esRutaPermitida(request)) {
            response.sendRedirect(request.getContextPath() + "/verificacion");
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean esRutaPermitida(HttpServletRequest request) {
        String ruta = request.getRequestURI().substring(request.getContextPath().length());
        return ruta.equals("/verificacion") || ruta.equals("/logout") || ruta.equals("/login")
                || ruta.equals("/error") || ruta.startsWith("/css/") || ruta.startsWith("/js/")
                || ruta.startsWith("/img/") || ruta.startsWith("/uploads/");
    }
}
