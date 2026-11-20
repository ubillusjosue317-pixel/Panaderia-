package com.utp.panaderia.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Base64;
import java.util.Deque;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.utp.panaderia.model.TokenRecuperacion;
import com.utp.panaderia.model.Usuario;
import com.utp.panaderia.repository.TokenRecuperacionRepository;
import com.utp.panaderia.repository.UsuarioRepository;
import com.utp.panaderia.security.BloqueoIntentos;
import com.utp.panaderia.service.AuditoriaService;
import com.utp.panaderia.service.CorreoService;
import com.utp.panaderia.service.RecuperacionService;

@Service
public class RecuperacionServiceImpl implements RecuperacionService {

    private static final SecureRandom ALEATORIO = new SecureRandom();
    private static final Duration VIGENCIA = Duration.ofMinutes(30);
    private static final Duration VENTANA = Duration.ofMinutes(15);
    private static final String ERROR_TOKEN = "El enlace no es válido o ya venció. Solicita uno nuevo.";

    // solicitudes recientes por correo (en memoria), para que nadie llene la bandeja de otra persona
    private final Map<String, Deque<Instant>> solicitudes = new ConcurrentHashMap<>();

    @Value("${app.base-url}")
    private String baseUrl;

    // maximo de enlaces que se pueden pedir para un mismo correo en 15 minutos
    @Value("${app.recuperacion.max-solicitudes:3}")
    private int maxSolicitudes;

    @Autowired
    private TokenRecuperacionRepository tokenRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CorreoService correoService;

    @Autowired
    private BloqueoIntentos bloqueo;

    @Autowired
    private AuditoriaService auditoriaService;

    @Override
    @Transactional
    public void solicitar(String email) {
        String correo = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (correo.isEmpty() || correo.length() > 100 || !permitir(correo)) {
            return;
        }
        usuarioRepository.findByEmail(correo).ifPresent(usuario -> {
            // un solo enlace vigente a la vez: el anterior deja de servir
            tokenRepository.invalidarPendientes(usuario.getId());

            // 256 bits aleatorios, imposibles de adivinar
            byte[] bytes = new byte[32];
            ALEATORIO.nextBytes(bytes);
            String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

            TokenRecuperacion registro = new TokenRecuperacion();
            registro.setUsuario(usuario);
            registro.setTokenHash(hash(token));
            registro.setExpiraEn(LocalDateTime.now().plus(VIGENCIA));
            tokenRepository.save(registro);

            // el enlace usa la direccion configurada (app.base-url), NO la cabecera Host de la peticion:
            // asi nadie puede hacer que el correo apunte a un sitio falso
            correoService.enviarRecuperacion(usuario.getEmail(), usuario.getNombre(),
                    baseUrl + "/recuperar/restablecer?token=" + token);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public boolean tokenValido(String token) {
        return buscarVigente(token) != null;
    }

    @Override
    @Transactional
    public void restablecer(String token, String nuevaPassword) {
        TokenRecuperacion registro = buscarVigente(token);
        if (registro == null || tokenRepository.marcarUsado(registro.getId()) == 0) {
            throw new IllegalArgumentException(ERROR_TOKEN);
        }
        Usuario usuario = registro.getUsuario();
        usuario.setPassword(passwordEncoder.encode(nuevaPassword));
        usuario.setDebeCambiarPassword(false);
        usuarioRepository.save(usuario);
        tokenRepository.invalidarPendientes(usuario.getId());
        bloqueo.limpiar(usuario.getEmail());
        auditoriaService.registrar("RESTABLECER_CLAVE", "Usuario", usuario.getId(),
                "Contraseña restablecida por enlace de recuperación (" + usuario.getEmail() + ")");
    }

    private TokenRecuperacion buscarVigente(String token) {
        if (token == null || token.isBlank() || token.length() > 100) {
            return null;
        }
        return tokenRepository.findByTokenHash(hash(token))
                .filter(t -> !t.isUsado() && t.getExpiraEn().isAfter(LocalDateTime.now()))
                .orElse(null);
    }

    private boolean permitir(String correo) {
        Deque<Instant> recientes = solicitudes.computeIfAbsent(correo, k -> new ArrayDeque<>());
        synchronized (recientes) {
            Instant limite = Instant.now().minus(VENTANA);
            while (!recientes.isEmpty() && recientes.peekFirst().isBefore(limite)) {
                recientes.pollFirst();
            }
            if (recientes.size() >= maxSolicitudes) {
                return false;
            }
            recientes.addLast(Instant.now());
            return true;
        }
    }

    private static String hash(String token) {
        try {
            byte[] resumen = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(resumen);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
