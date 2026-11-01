package pe.edu.pucp.laboratorio7.order;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Cálculos monetarios del pedido (siempre en Java, nunca en JavaScript). */
public final class OrderMath {

    private OrderMath() {
    }

    /** Redondea a 2 decimales (evita errores de coma flotante como 59.970000000000006). */
    public static double round2(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    /** Subtotal de una línea = cantidad × precio unitario. */
    public static double subtotal(int quantity, double unitPrice) {
        return round2(quantity * unitPrice);
    }
}
