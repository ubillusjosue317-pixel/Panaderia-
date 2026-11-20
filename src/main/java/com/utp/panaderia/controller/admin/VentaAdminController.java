package com.utp.panaderia.controller.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.utp.panaderia.service.ExcelVentasException;
import com.utp.panaderia.service.VentaService;

// Carga de ventas desde Excel. Solo el administrador (SecurityConfig protege /admin/**).
@Controller
@RequestMapping("/admin/ventas")
public class VentaAdminController {

    @Autowired
    private VentaService ventaService;

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("ventas", ventaService.ultimas());
        model.addAttribute("cantidadVentas", ventaService.contar());
        model.addAttribute("totalVentas", ventaService.totalGeneral());
        return "admin/ventas/lista";
    }

    @PostMapping("/cargar")
    public String cargar(@RequestParam(required = false) MultipartFile archivo, RedirectAttributes redirect) {
        try {
            int cargadas = ventaService.cargarExcel(archivo);
            redirect.addFlashAttribute("mensaje", "Se cargaron " + cargadas + " ventas correctamente.");
        } catch (ExcelVentasException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            redirect.addFlashAttribute("errores", e.getErrores());
        }
        return "redirect:/admin/ventas";
    }

    @GetMapping("/plantilla.xlsx")
    public ResponseEntity<byte[]> plantilla() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("plantilla-ventas.xlsx").build().toString())
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(ventaService.plantilla());
    }
}
