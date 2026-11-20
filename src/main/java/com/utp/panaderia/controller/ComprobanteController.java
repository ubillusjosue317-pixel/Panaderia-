package com.utp.panaderia.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.utp.panaderia.model.Rol;
import com.utp.panaderia.security.UsuarioAutenticado;
import com.utp.panaderia.service.ComprobanteService;
import com.utp.panaderia.util.FirmaArchivo;

@Controller
public class ComprobanteController {

    @Autowired
    private ComprobanteService comprobanteService;

    @PostMapping("/mis-pedidos/{id}/comprobante")
    public String subir(@PathVariable Integer id, @RequestParam(required = false) MultipartFile archivo,
                        @AuthenticationPrincipal UsuarioAutenticado usuario, RedirectAttributes redirect) {
        try {
            comprobanteService.guardar(usuario.getId(), id, archivo);
            redirect.addFlashAttribute("mensaje", "Recibimos tu comprobante del pedido #" + id + ".");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/mis-pedidos";
    }

    // El comprobante NO esta en una carpeta publica: se entrega por aqui, solo a su dueño o al administrador
    @GetMapping("/comprobantes/{id}")
    public ResponseEntity<byte[]> ver(@PathVariable Integer id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        boolean esAdmin = usuario.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + Rol.ADMINISTRADOR));
        ComprobanteService.Archivo archivo;
        try {
            archivo = comprobanteService.obtener(usuario.getId(), esAdmin, id);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
        // las imagenes se ven en el navegador; el PDF se descarga
        boolean pdf = FirmaArchivo.PDF.equals(archivo.tipoMime());
        ContentDisposition disposicion = (pdf ? ContentDisposition.attachment() : ContentDisposition.inline())
                .filename("comprobante-" + id + "." + archivo.extension()).build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposicion.toString())
                .header("Content-Security-Policy", "default-src 'none'; style-src 'unsafe-inline'; sandbox")
                .contentType(MediaType.parseMediaType(archivo.tipoMime()))
                .body(archivo.contenido());
    }
}
