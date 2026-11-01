package pe.edu.pucp.laboratorio7.customer;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Regla que depende de dos campos: DNI = exactamente 8 dígitos, RUC = exactamente 11 dígitos.
 * El error se asocia al campo "document" para mostrarse junto al input.
 */
@Documented
@Constraint(validatedBy = CustomerDocumentValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidCustomerDocument {

    String message() default "Documento inválido";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
