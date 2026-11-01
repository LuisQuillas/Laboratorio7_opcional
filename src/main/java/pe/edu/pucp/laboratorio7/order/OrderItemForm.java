package pe.edu.pucp.laboratorio7.order;

/** Una línea del formulario de pedido: producto + cantidad solicitada. */
public class OrderItemForm {

    private Integer productId;
    private Integer quantity;

    public Integer getProductId() {
        return productId;
    }

    public void setProductId(Integer productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
