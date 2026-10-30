package com.utp.panaderia.service;

import org.springframework.web.multipart.MultipartFile;

public interface ImagenService {

    // Guarda la foto subida desde el formulario y devuelve la ruta para mostrarla (ej. /uploads/productos/abc.jpg)
    String guardarImagenProducto(MultipartFile archivo);
}
