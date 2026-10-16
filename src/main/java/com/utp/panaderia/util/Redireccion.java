package com.utp.panaderia.util;

import java.net.URI;

public class Redireccion {

    private Redireccion() {
    }

    // Regresa a la pagina desde donde se hizo clic (por ejemplo al agregar al carrito desde Explorar).
    // Solo se usa la ruta del Referer, nunca otro dominio, para evitar redirecciones a sitios externos.
    public static String volver(String referer, String porDefecto) {
        if (referer == null) {
            return "redirect:" + porDefecto;
        }
        try {
            URI uri = URI.create(referer);
            String ruta = uri.getRawPath();
            if (ruta == null || !ruta.startsWith("/") || ruta.startsWith("//")) {
                return "redirect:" + porDefecto;
            }
            if (uri.getRawQuery() != null) {
                ruta = ruta + "?" + uri.getRawQuery();
            }
            return "redirect:" + ruta;
        } catch (IllegalArgumentException e) {
            return "redirect:" + porDefecto;
        }
    }
}
