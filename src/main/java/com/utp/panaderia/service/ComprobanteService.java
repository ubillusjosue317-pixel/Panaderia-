package com.utp.panaderia.service;

import org.springframework.web.multipart.MultipartFile;

// Comprobante de pago (captura de Yape / Plin) que el cliente sube para su pedido
public interface ComprobanteService {

    // Contenido ya leido de un comprobante, con el tipo REAL del archivo
    record Archivo(byte[] contenido, String tipoMime, String extension) {
    }

    // Valida el archivo (extension, tipo real, tamaño) y lo guarda con nombre aleatorio
    void guardar(Integer idUsuario, Integer idPedido, MultipartFile archivo);

    // Solo el dueño del pedido o un administrador pueden verlo
    Archivo obtener(Integer idUsuario, boolean esAdmin, Integer idPedido);
}
