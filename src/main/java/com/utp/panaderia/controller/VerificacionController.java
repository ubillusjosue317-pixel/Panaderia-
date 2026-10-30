package com.utp.panaderia.controller;

import static com.utp.panaderia.security.VerificacionDosPasosFilter.DESTINO;
import static com.utp.panaderia.security.VerificacionDosPasosFilter.INTENTOS;
import static com.utp.panaderia.security.VerificacionDosPasosFilter.PENDIENTE;
import static com.utp.panaderia.security.VerificacionDosPasosFilter.SECRETO_NUEVO;

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
// La primera vez se muestra el QR para registrar la cuenta en la app; las siguientes solo se pide el codigo.
@Controller
public class VerificacionController {

    private static final String EMISOR = "Panadería Piero Alonso";
    private static final int MAX_INTENTOS = 5;

    @Autowired
    private UsuarioService usuarioService;

    @GetMapping("/verificacion")
    public String formulario(@AuthenticationPrincipal UsuarioAutenticado autenticado, HttpSession sesion,
                             Model model) {
        if (sesion.getAttribute(PENDIENTE) == null) {
            return "redirect:/";
        }

        Usuario usuario = usuarioService.buscarPorId(autenticado.getId());
        if (usuario.getTotpSecreto() == null) {
            // se genera una sola vez por sesion, asi el QR no cambia si recarga la pagina
            String secreto = (String) sesion.getAttribute(SECRETO_NUEVO);
            if (secreto == null) {
                secreto = Totp.generarSecreto();
                sesion.setAttribute(SECRETO_NUEVO, secreto);
            }
            model.addAttribute("qr", CodigoQr.comoImagen(Totp.enlaceQr(EMISOR, usuario.getEmail(), secreto), 220));
            model.addAttribute("secreto", secreto.replaceAll("(.{4})(?!$)", "$1 "));
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
        boolean primeraVez = usuario.getTotpSecreto() == null;
        String secreto = primeraVez ? (String) sesion.getAttribute(SECRETO_NUEVO) : usuario.getTotpSecreto();

        if (Totp.esValido(secreto, codigo.replaceAll("\\s", ""))) {
            if (primeraVez) {
                usuarioService.activarDosPasos(usuario.getId(), secreto);
            }
            String destino = (String) sesion.getAttribute(DESTINO);
            sesion.removeAttribute(PENDIENTE);
            sesion.removeAttribute(DESTINO);
            sesion.removeAttribute(SECRETO_NUEVO);
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
}
