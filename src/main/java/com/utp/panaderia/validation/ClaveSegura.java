package com.utp.panaderia.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

// Contraseña con mayuscula, minuscula, numero y simbolo, y que no sea una clave comun
@Documented
@Constraint(validatedBy = ClaveSeguraValidator.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ClaveSegura {

    String message() default "Contraseña no válida";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
