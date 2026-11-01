package pe.edu.pucp.laboratorio7.customer;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CustomerDocumentValidator implements ConstraintValidator<ValidCustomerDocument, Customer> {

    @Override
    public boolean isValid(Customer customer, ConstraintValidatorContext context) {
        if (customer == null) {
            return true;
        }
        String document = customer.getDocument();
        String type = customer.getDocumentType();

        // Campo vacío o tipo inválido: ya lo reportan @NotBlank / @Pattern
        if (document == null || document.isBlank() || type == null) {
            return true;
        }

        String message;
        if ("DNI".equals(type) && !document.matches("\\d{8}")) {
            message = "El DNI debe contener exactamente 8 dígitos";
        } else if ("RUC".equals(type) && !document.matches("\\d{11}")) {
            message = "El RUC debe contener exactamente 11 dígitos";
        } else {
            return true;
        }

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode("document")
                .addConstraintViolation();
        return false;
    }
}
