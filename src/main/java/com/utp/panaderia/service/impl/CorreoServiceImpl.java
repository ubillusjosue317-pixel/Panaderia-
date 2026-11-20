package com.utp.panaderia.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.utp.panaderia.service.CorreoService;

// Envia por correo si hay un servidor SMTP configurado (variables SPRING_MAIL_HOST, SPRING_MAIL_USERNAME,
// SPRING_MAIL_PASSWORD...). Si no lo hay (computadora de desarrollo), el enlace se escribe en la consola.
@Service
public class CorreoServiceImpl implements CorreoService {

    private static final Logger log = LoggerFactory.getLogger(CorreoServiceImpl.class);

    @Autowired
    private ObjectProvider<JavaMailSender> correo;

    @Value("${app.mail.from}")
    private String remitente;

    @Override
    public void enviarRecuperacion(String destino, String nombre, String enlace) {
        JavaMailSender servidor = correo.getIfAvailable();
        if (servidor == null) {
            log.warn("MODO DESARROLLO (no hay servidor de correo configurado). Enlace de recuperacion para {}: {}",
                    destino, enlace);
            return;
        }
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente);
        mensaje.setTo(destino);
        mensaje.setSubject("Restablece tu contraseña - Panadería Piero Alonso");
        mensaje.setText("Hola " + nombre + ",\n\n"
                + "Recibimos una solicitud para restablecer tu contraseña. Abre este enlace:\n\n"
                + enlace + "\n\n"
                + "El enlace vence en 30 minutos y solo se puede usar una vez.\n"
                + "Si no fuiste tú, ignora este mensaje: tu contraseña no cambiará.\n");
        try {
            servidor.send(mensaje);
        } catch (MailException e) {
            // no se revela al usuario si el correo existe o si fallo el envio
            log.error("No se pudo enviar el correo de recuperacion: {}", e.getMessage());
        }
    }
}
