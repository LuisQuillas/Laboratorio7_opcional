package pe.edu.pucp.laboratorio7.order;

/** Regla de negocio incumplida al registrar un pedido (por ejemplo, stock insuficiente por concurrencia). */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
