package com.utp.panaderia.util;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;

import javax.imageio.ImageIO;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

public class CodigoQr {

    private CodigoQr() {
    }

    // Devuelve el QR como imagen PNG lista para poner en <img src="...">,
    // asi no se guarda ningun archivo ni se depende de un servicio externo
    public static String comoImagen(String texto, int tamano) {
        try {
            BitMatrix matriz = new QRCodeWriter().encode(texto, BarcodeFormat.QR_CODE, tamano, tamano,
                    Map.of(EncodeHintType.MARGIN, 1));

            BufferedImage imagen = new BufferedImage(tamano, tamano, BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < tamano; x++) {
                for (int y = 0; y < tamano; y++) {
                    imagen.setRGB(x, y, matriz.get(x, y) ? 0x000000 : 0xFFFFFF);
                }
            }

            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            ImageIO.write(imagen, "png", salida);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(salida.toByteArray());
        } catch (WriterException | IOException e) {
            throw new IllegalStateException("No se pudo generar el codigo QR", e);
        }
    }
}
