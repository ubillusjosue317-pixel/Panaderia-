package com.utp.panaderia.util;

// Reconoce el tipo REAL de un archivo por sus primeros bytes ("firma"), sin fiarse de la extension
// ni del tipo que declara el navegador (ambos los puede falsear quien sube el archivo).
public final class FirmaArchivo {

    public static final String JPEG = "image/jpeg";
    public static final String PNG = "image/png";
    public static final String WEBP = "image/webp";
    public static final String PDF = "application/pdf";

    // bytes que hay que leer para poder reconocer cualquiera de los formatos
    public static final int BYTES_NECESARIOS = 12;

    private FirmaArchivo() {
    }

    // Devuelve el tipo real o null si no es JPG, PNG, WEBP ni PDF
    public static String detectar(byte[] b) {
        if (b == null) {
            return null;
        }
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return JPEG;
        }
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
                && b[4] == 0x0D && b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A) {
            return PNG;
        }
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return WEBP;
        }
        if (b.length >= 5 && b[0] == '%' && b[1] == 'P' && b[2] == 'D' && b[3] == 'F' && b[4] == '-') {
            return PDF;
        }
        return null;
    }
}
