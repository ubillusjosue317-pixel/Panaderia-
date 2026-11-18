package com.utp.panaderia.controller;

import static com.utp.panaderia.security.VerificacionDosPasosFilter.CAMBIO_CLAVE;
import static com.utp.panaderia.security.VerificacionDosPasosFilter.DESTINO;
import static com.utp.panaderia.security.VerificacionDosPasosFilter.INTENTOS;
import static com.utp.panaderia.security.VerificacionDosPasosFilter.PENDIENTE;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.utp.panaderia.model.Usuario;
import com.utp.panaderia.security.UsuarioAutenticado;
import com.utp.panaderia.service.UsuarioService;
import com.utp.panaderia.util.CodigoQr;
import com.utp.panaderia.util.Totp;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

// Segundo paso del inicio de sesion (obligatorio para todos).
// El QR se escanea al registrarse; el administrador lo ve aqui solo hasta confirmar su primer codigo.
@Controller
public class VerificacionController {

    private static final String EMISOR = "Panadería Piero Alonso";
    private static final int MAX_INTENTOS = 5;

    @Autowired
    private UsuarioService usuarioService;

    @GetMapping("/verificacion")
    public String formulario(@AuthenticationPrincipal UsuarioAutenticado autenticado, HttpSession sesion,
                             HttpServletRequest request, HttpServletResponse response, Model model) {
        if (sesion.getAttribute(PENDIENTE) == null) {
            return "redirect:/";
        }

        Usuario usuario = usuarioService.buscarPorId(autenticado.getId());
        if (usuario.getTotpSecreto() == null) {
            return sinClave(request, response);
        }
        if (!usuario.isTotpConfirmado()) {
            // administrador recien creado: aun no vincula su Authenticator, se le muestra el QR
            model.addAttribute("qr", CodigoQr.comoImagen(
                    Totp.enlaceQr(EMISOR, usuario.getEmail(), usuario.getTotpSecreto()), 220));
            model.addAttribute("secreto", usuario.getTotpSecreto().replaceAll("(.{4})(?!$)", "$1 "));
        }
        return "auth/verificacion";
    }

    @PostMapping("/verificacion")
    public String verificar(@AuthenticationPrincipal UsuarioAutenticado autenticado,
                            @RequestParam(defaultValue = "") String codigo,
                            HttpServletRequest request, HttpServletResponse response,
                            RedirectAttributes redirect) {
        HttpSession sesion = request.getSession();
        if (sesion.getAttribute(PENDIENTE) == null) {
            return "redirect:/";
        }

        Usuario usuario = usuarioService.buscarPorId(autenticado.getId());
        if (usuario.getTotpSecreto() == null) {
            return sinClave(request, response);
        }

        if (Totp.esValido(usuario.getTotpSecreto(), codigo.replaceAll("\s", ""))) {
            if (!usuario.isTotpConfirmado()) {
                usuarioService.confirmarDosPasos(usuario.getId());
            }
            String destino = (String) sesion.getAttribute(DESTINO);
            sesion.removeAttribute(PENDIENTE);
            if (usuario.isDebeCambiarPassword()) {
                // no puede usar el sistema hasta cambiar su contraseña; el destino se conserva
                sesion.setAttribute(CAMBIO_CLAVE, true);
                request.changeSessionId();
                return "redirect:/cambiar-clave";
            }
            sesion.removeAttribute(DESTINO);
            sesion.removeAttribute(INTENTOS);
            // nuevo id de sesion al completar el inicio de sesion
            request.changeSessionId();
            return "redirect:" + (destino != null ? destino : "/");
        }

        Integer intentos = (Integer) sesion.getAttribute(INTENTOS);
        intentos = (intentos == null ? 0 : intentos) + 1;
        if (intentos >= MAX_INTENTOS) {
            // demasiados errores: se cierra la sesion y tiene que volver a poner la contraseña
            new SecurityContextLogoutHandler().logout(request, response,
                    SecurityContextHolder.getContext().getAuthentication());
            return "redirect:/login?bloqueado";
        }
        sesion.setAttribute(INTENTOS, intentos);
        redirect.addFlashAttribute("error",
                "Código incorrecto. Te quedan " + (MAX_INTENTOS - intentos) + " intentos.");
        return "redirect:/verificacion";
    }

    // La clave del Authenticator se crea al registrarse; una cuenta sin ella no puede entrar
    // (asi nadie mas puede vincular su celular a una cuenta ajena en el primer inicio de sesion)
    private String sinClave(HttpServletRequest request, HttpServletResponse response) {
        new SecurityContextLogoutHandler().logout(request, response,
                SecurityContextHolder.getContext().getAuthentication());
        return "redirect:/login?sinClave";
    }
}
