package com.utp.panaderia.validation;

import java.util.List;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ClaveSeguraValidator implements ConstraintValidator<ClaveSegura, String> {

    // si la clave contiene alguna de estas palabras (sin importar mayusculas) se rechaza
    private static final List<String> COMUNES = List.of(
            "password", "passw0rd", "qwerty", "admin", "letmein", "welcome", "iloveyou",
            "panaderia", "pieroalonso", "abc123", "123456", "654321", "111111", "000000");

    @Override
    public boolean isValid(String clave, ConstraintValidatorContext contexto) {
        if (clave == null || clave.isEmpty()) {
            return true; // de eso se encarga @NotBlank
        }

        boolean completa = clave.chars().anyMatch(Character::isUpperCase)
                && clave.chars().anyMatch(Character::isLowerCase)
                && clave.chars().anyMatch(Character::isDigit)
                && clave.chars().anyMatch(c -> !Character.isLetterOrDigit(c) && !Character.isWhitespace(c));
        if (!completa) {
            return rechazar(contexto,
                    "Debe incluir al menos una mayúscula, una minúscula, un número y un símbolo");
        }

        String minuscula = clave.toLowerCase();
        if (COMUNES.stream().anyMatch(minuscula::contains)) {
            return rechazar(contexto, "Esa contraseña es muy común, elige otra");
        }
        return true;
    }

    private boolean rechazar(ConstraintValidatorContext contexto, String mensaje) {
        contexto.disableDefaultConstraintViolation();
        contexto.buildConstraintViolationWithTemplate(mensaje).addConstraintViolation();
        return false;
    }
}
