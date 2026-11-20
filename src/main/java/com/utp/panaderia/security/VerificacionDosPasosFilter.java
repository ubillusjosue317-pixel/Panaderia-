package com.utp.panaderia.security;

import java.io.IOException;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

// Despues de poner bien la contraseña la sesion queda "pendiente" hasta que el usuario
// escriba el codigo de su app autenticadora. Mientras tanto solo puede abrir /verificacion.
// Si ademas tiene que cambiar su contraseña (primer ingreso del administrador), tampoco puede
// abrir otra pagina que /cambiar-clave hasta hacerlo.
public class VerificacionDosPasosFilter extends OncePerRequestFilter {

    // atributos que se guardan en la sesion
    public static final String PENDIENTE = "dosPasos.pendiente";
    public static final String DESTINO = "dosPasos.destino";
    public static final String INTENTOS = "dosPasos.intentos";
    public static final String INICIO = "dosPasos.inicio";
    public static final String CAMBIO_CLAVE = "cambioClave.pendiente";
    private static final long LIMITE_MS = 5 * 60 * 1000L;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        HttpSession sesion = request.getSession(false);
        if (sesion != null) {
            // el segundo paso tiene que completarse en pocos minutos; si no, hay que empezar de nuevo
            if (sesion.getAttribute(PENDIENTE) != null
                    && sesion.getAttribute(INICIO) instanceof Long inicio
                    && System.currentTimeMillis() - inicio > LIMITE_MS) {
                sesion.invalidate();
                response.sendRedirect(request.getContextPath() + "/login?expirada");
                return;
            }
            String ruta = request.getRequestURI().substring(request.getContextPath().length());

            // Si estando a medias (falta el codigo o el cambio de clave) la persona vuelve atras
            // y abre el login o el registro, se cancela esa sesion a medias y puede empezar de nuevo
            // (por ejemplo para crear otra cuenta), en lugar de quedar atrapada en la verificacion.
            boolean aMedias = sesion.getAttribute(PENDIENTE) != null || sesion.getAttribute(CAMBIO_CLAVE) != null;
            if (aMedias && "GET".equalsIgnoreCase(request.getMethod())
                    && (ruta.equals("/login") || ruta.equals("/registro") || ruta.equals("/recuperar"))) {
                sesion.invalidate();
                SecurityContextHolder.clearContext();
                chain.doFilter(request, response);
                return;
            }
            if (sesion.getAttribute(PENDIENTE) != null && !ruta.equals("/verificacion") && !esComun(ruta)) {
                response.sendRedirect(request.getContextPath() + "/verificacion");
                return;
            }
            if (sesion.getAttribute(CAMBIO_CLAVE) != null && !ruta.equals("/cambiar-clave") && !esComun(ruta)) {
                response.sendRedirect(request.getContextPath() + "/cambiar-clave");
                return;
            }
        }
        chain.doFilter(request, response);
    }

    // rutas que siempre se pueden abrir: salir, errores y archivos estaticos
    private boolean esComun(String ruta) {
        return ruta.equals("/logout") || ruta.equals("/login") || ruta.equals("/error")
                || ruta.startsWith("/css/") || ruta.startsWith("/js/") || ruta.startsWith("/img/")
                || ruta.startsWith("/vendor/") || ruta.startsWith("/uploads/");
    }
}
