package com.utp.panaderia.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.utp.panaderia.service.ImagenService;

@Service
public class ImagenServiceImpl implements ImagenService {

    private static final long MAXIMO_BYTES = 5 * 1024 * 1024;

    // solo se aceptan estos formatos (tipo que manda el navegador -> extension con la que se guarda)
    private static final Map<String, String> FORMATOS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp");

    @Value("${app.uploads.dir}")
    private String carpetaUploads;

    @Override
    public String guardarImagenProducto(MultipartFile archivo) {
        String extension = FORMATOS.get(archivo.getContentType());
        if (extension == null) {
            throw new IllegalArgumentException("La imagen debe ser JPG, PNG o WEBP.");
        }
        if (archivo.getSize() > MAXIMO_BYTES) {
            throw new IllegalArgumentException("La imagen no puede pesar más de 5 MB.");
        }

        // nombre aleatorio: no se usa el nombre original para evitar rutas raras o archivos repetidos
        String nombre = UUID.randomUUID() + extension;
        Path carpeta = Paths.get(carpetaUploads, "productos").toAbsolutePath().normalize();
        try (InputStream entrada = archivo.getInputStream()) {
            Files.createDirectories(carpeta);
            Files.copy(entrada, carpeta.resolve(nombre), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar la imagen", e);
        }
        return "/uploads/productos/" + nombre;
    }
}
