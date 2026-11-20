package com.utp.panaderia.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;

import com.utp.panaderia.model.Usuario;
import com.utp.panaderia.repository.UsuarioRepository;

// Simula una base de datos creada con la version anterior: el administrador sigue con "Admin12345".
// Al arrancar de nuevo, esa clave se reemplaza (en el test el admin se crea con esa misma clave).
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AdminAntiguoTest {

    @Autowired
    private DatosIniciales datosIniciales;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void elAdminConLaClaveAntiguaSeReemplazaAlArrancar() {
        Usuario antes = usuarioRepository.findByEmail("admin@pieroalonso.pe").orElseThrow();
        assertTrue(passwordEncoder.matches("Admin12345", antes.getPassword()));
        String secretoAntes = antes.getTotpSecreto();

        datosIniciales.run();

        Usuario despues = usuarioRepository.findByEmail("admin@pieroalonso.pe").orElseThrow();
        assertFalse(passwordEncoder.matches("Admin12345", despues.getPassword()));
        assertNotEquals(secretoAntes, despues.getTotpSecreto());
        assertTrue(despues.isDebeCambiarPassword());
        assertFalse(despues.isTotpConfirmado());
    }
}
