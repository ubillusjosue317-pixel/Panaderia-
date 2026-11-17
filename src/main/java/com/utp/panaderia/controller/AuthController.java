package com.utp.panaderia.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.utp.panaderia.dto.RegistroForm;
import com.utp.panaderia.dto.RegistroPendiente;
import com.utp.panaderia.service.UsuarioService;
import com.utp.panaderia.util.CodigoQr;
import com.utp.panaderia.util.Totp;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
public class AuthController {

    // donde se guarda el registro mientras la persona escanea el QR
    public static final String REGISTRO_PENDIENTE = "registro.pendiente";

    private static final String EMISOR = "Panadería Piero Alonso";
    private static final int MAX_INTENTOS = 5;
    private static final long LIMITE_MS = 10 * 60 * 1000L;

    @Autowired
    private UsuarioService usuarioService;

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/registro")
    public String registro(Model model) {
        model.addAttribute("registro", new RegistroForm());
        return "auth/registro";
    }

    // Paso 1: datos de la cuenta. Todavia no se crea nada: se pasa a escanear el QR.
    @PostMapping("/registro")
    public String registrar(@Valid @ModelAttribute("registro") RegistroForm form, BindingResult result,
                            HttpSession sesion) {

        if (form.getPassword() != null && !form.getPassword().equals(form.getConfirmarPassword())) {
            result.rejectValue("confirmarPassword", "noCoincide", "Las contraseñas no coinciden");
        }
        if (result.hasErrors()) {
            return "auth/registro";
        }

        // el flujo es igual exista o no el correo, para no revelar que cuentas existen
        RegistroPendiente pendiente = new RegistroPendiente();
        pendiente.setNombre(form.getNombre());
        pendiente.setApellido(form.getApellido());
        pendiente.setEmail(form.getEmail().trim().toLowerCase());
        pendiente.setTelefono(form.getTelefono());
        pendiente.setPasswordCifrada(usuarioService.cifrarPassword(form.getPassword()));
        pendiente.setSecreto(Totp.generarSecreto());
        sesion.setAttribute(REGISTRO_PENDIENTE, pendiente);
        return "redirect:/registro/verificacion";
    }

    // Paso 2: escanear el QR con la app autenticadora. Solo quien se registra lo ve.
    @GetMapping("/registro/verificacion")
    public String verificacion(HttpSession sesion, Model model) {
        RegistroPendiente pendiente = vigente(sesion);
        if (pendiente == null) {
            return "redirect:/registro";
        }
        model.addAttribute("qr", CodigoQr.comoImagen(
                Totp.enlaceQr(EMISOR, pendiente.getEmail(), pendiente.getSecreto()), 220));
        model.addAttribute("secreto", pendiente.getSecreto().replaceAll("(.{4})(?!$)", "$1 "));
        return "auth/registro-verificacion";
    }

    // Paso 3: con un codigo valido recien se crea la cuenta, ya con su clave de 2 pasos
    @PostMapping("/registro/verificacion")
    public String confirmar(@RequestParam(defaultValue = "") String codigo, HttpSession sesion,
                            RedirectAttributes redirect) {
        RegistroPendiente pendiente = vigente(sesion);
        if (pendiente == null) {
            return "redirect:/registro";
        }

        if (!Totp.esValido(pendiente.getSecreto(), codigo.replaceAll("\\s", ""))) {
            pendiente.setIntentos(pendiente.getIntentos() + 1);
            if (pendiente.getIntentos() >= MAX_INTENTOS) {
                sesion.removeAttribute(REGISTRO_PENDIENTE);
                return "redirect:/registro";
            }
            redirect.addFlashAttribute("error", "Código incorrecto. Te quedan "
                    + (MAX_INTENTOS - pendiente.getIntentos()) + " intentos.");
            return "redirect:/registro/verificacion";
        }

        if (!usuarioService.existeEmail(pendiente.getEmail())) {
            try {
                usuarioService.crearCliente(pendiente);
            } catch (DataIntegrityViolationException e) {
                // otro registro con el mismo correo llego al mismo tiempo: se responde igual
            }
        }
        sesion.removeAttribute(REGISTRO_PENDIENTE);
        redirect.addFlashAttribute("mensaje",
                "Recibimos tu registro. Si el correo puede usarse, ya puedes iniciar sesión.");
        return "redirect:/login";
    }

    private RegistroPendiente vigente(HttpSession sesion) {
        RegistroPendiente pendiente = (RegistroPendiente) sesion.getAttribute(REGISTRO_PENDIENTE);
        if (pendiente == null) {
            return null;
        }
        if (System.currentTimeMillis() - pendiente.getCreadoEn() > LIMITE_MS) {
            sesion.removeAttribute(REGISTRO_PENDIENTE);
            return null;
        }
        return pendiente;
    }

    @GetMapping("/acceso-denegado")
    public String accesoDenegado() {
        return "error/acceso-denegado";
    }
}
