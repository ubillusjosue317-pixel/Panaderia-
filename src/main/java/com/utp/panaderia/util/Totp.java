package com.utp.panaderia.util;

import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Instant;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base32;

// Codigos de 6 digitos que cambian cada 30 segundos (estandar TOTP, RFC 6238).
// Es lo mismo que calculan Google Authenticator o Microsoft Authenticator con la clave del QR.
public class Totp {

    private static final int SEGUNDOS = 30;
    private static final SecureRandom random = new SecureRandom();

    private Totp() {
    }

    // Clave aleatoria de 160 bits en Base32 (32 letras/numeros), es lo que guarda el celular
    public static String generarSecreto() {
        byte[] bytes = new byte[20];
        random.nextBytes(bytes);
        return new Base32().encodeToString(bytes).replace("=", "");
    }

    // Texto que va dentro del QR. Las apps lo leen y crean la cuenta "Panaderia Piero Alonso (correo)"
    public static String enlaceQr(String emisor, String email, String secreto) {
        String e = codificar(emisor);
        return "otpauth://totp/" + e + ":" + codificar(email)
                + "?secret=" + secreto + "&issuer=" + e + "&digits=6&period=" + SEGUNDOS;
    }

    // Acepta el codigo actual y el de 30 segundos antes o despues, por si el reloj del celular no esta exacto
    public static boolean esValido(String secreto, String codigo) {
        if (secreto == null || codigo == null || !codigo.matches("\\d{6}")) {
            return false;
        }
        long paso = Instant.now().getEpochSecond() / SEGUNDOS;
        for (int i = -1; i <= 1; i++) {
            if (calcular(secreto, paso + i).equals(codigo)) {
                return true;
            }
        }
        return false;
    }

    public static String codigoActual(String secreto) {
        return calcular(secreto, Instant.now().getEpochSecond() / SEGUNDOS);
    }

    private static String calcular(String secreto, long paso) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(new Base32().decode(secreto), "HmacSHA1"));
            byte[] hash = mac.doFinal(ByteBuffer.allocate(8).putLong(paso).array());

            // "truncado dinamico" del RFC: se toman 4 bytes del hash y se dejan 6 digitos
            int inicio = hash[hash.length - 1] & 0x0F;
            int numero = ((hash[inicio] & 0x7F) << 24)
                    | ((hash[inicio + 1] & 0xFF) << 16)
                    | ((hash[inicio + 2] & 0xFF) << 8)
                    | (hash[inicio + 3] & 0xFF);
            return String.format("%06d", numero % 1_000_000);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo calcular el codigo de verificacion", e);
        }
    }

    private static String codificar(String texto) {
        return URLEncoder.encode(texto, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
