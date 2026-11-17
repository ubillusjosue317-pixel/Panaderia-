package com.utp.panaderia.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

// Bloquea temporalmente un correo despues de 5 contraseñas incorrectas seguidas.
// Se cuenta por correo (exista o no) para no revelar que cuentas existen.
// Los datos viven en memoria: al reiniciar la app se limpian.
@Component
public class BloqueoIntentos {

    public static final int MAXIMO_INTENTOS = 5;
    public static final Duration DURACION = Duration.ofMinutes(15);

    private static class Estado {
        int fallos;
        Instant bloqueadoHasta;
    }

    private final Map<String, Estado> estados = new ConcurrentHashMap<>();

    public boolean estaBloqueado(String email) {
        Estado estado = estados.get(normalizar(email));
        if (estado == null) {
            return false;
        }
        synchronized (estado) {
            if (estado.bloqueadoHasta == null) {
                return false;
            }
            if (Instant.now().isAfter(estado.bloqueadoHasta)) {
                estados.remove(normalizar(email), estado);
                return false;
            }
            return true;
        }
    }

    public void registrarFallo(String email) {
        Estado estado = estados.computeIfAbsent(normalizar(email), k -> new Estado());
        synchronized (estado) {
            estado.fallos++;
            if (estado.fallos >= MAXIMO_INTENTOS) {
                estado.bloqueadoHasta = Instant.now().plus(DURACION);
            }
        }
    }

    public void limpiar(String email) {
        estados.remove(normalizar(email));
    }

    @EventListener
    public void alFallar(AuthenticationFailureBadCredentialsEvent evento) {
        registrarFallo(evento.getAuthentication().getName());
    }

    @EventListener
    public void alAcertar(AuthenticationSuccessEvent evento) {
        limpiar(evento.getAuthentication().getName());
    }

    private String normalizar(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
