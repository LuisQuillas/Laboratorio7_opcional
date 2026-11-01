package pe.edu.pucp.laboratorio7.order;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import pe.edu.pucp.laboratorio7.customer.Customer;
import pe.edu.pucp.laboratorio7.customer.CustomerRepository;
import pe.edu.pucp.laboratorio7.product.Product;
import pe.edu.pucp.laboratorio7.product.ProductRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class OrderService {

    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;

    public OrderService(InvoiceRepository invoiceRepository,
                        CustomerRepository customerRepository,
                        ProductRepository productRepository) {
        this.invoiceRepository = invoiceRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
    }

    /**
     * Reglas de negocio que complementan las anotaciones de OrderForm:
     * líneas (producto obligatorio y no repetido, cantidad entera > 0 y <= stock),
     * existencia del cliente y compatibilidad Factura/RUC - Boleta/DNI (error global).
     * Los errores de conversión (binding) ya vienen en el BindingResult y no se duplican.
     */
    @Transactional(readOnly = true)
    public void validate(OrderForm form, BindingResult result) {
        List<OrderItemForm> items = form.getItems();

        boolean hasRows = false;
        Set<Integer> seenProducts = new HashSet<>();

        for (int i = 0; i < items.size(); i++) {
            OrderItemForm item = items.get(i);
            String productField = "items[" + i + "].productId";
            String quantityField = "items[" + i + "].quantity";

            boolean rowUsed = item.getProductId() != null
                    || item.getQuantity() != null
                    || result.hasFieldErrors("items[" + i + "].*");
            if (!rowUsed) {
                continue; // fila vacía: se ignora
            }
            hasRows = true;

            Product product = null;
            if (item.getProductId() == null) {
                if (!result.hasFieldErrors(productField)) {
                    result.rejectValue(productField, "order.item.product.required", "Seleccione un producto");
                }
            } else {
                product = productRepository.findById(item.getProductId()).orElse(null);
                if (product == null) {
                    result.rejectValue(productField, "order.item.product.notFound",
                            "El producto seleccionado no existe");
                } else if (!seenProducts.add(product.getId())) {
                    result.rejectValue(productField, "order.item.product.duplicated",
                            "El producto \"" + product.getName() + "\" ya fue incluido; no se puede repetir en un pedido");
                }
            }

            if (result.hasFieldErrors(quantityField)) {
                continue; // error de conversión (por ejemplo letras): ya tiene su mensaje
            }
            Integer quantity = item.getQuantity();
            if (quantity == null) {
                result.rejectValue(quantityField, "order.item.quantity.required", "La cantidad es obligatoria");
            } else if (quantity <= 0) {
                result.rejectValue(quantityField, "order.item.quantity.min",
                        "La cantidad debe ser un entero mayor que cero");
            } else if (product != null && quantity > product.getStock()) {
                result.rejectValue(quantityField, "order.item.quantity.stock",
                        "La cantidad (" + quantity + ") supera el stock disponible (" + product.getStock() + ")");
            }
        }

        if (!hasRows) {
            result.rejectValue("items", "order.items.required", "Debe incluir al menos un producto");
        }

        // Regla que depende de varios campos -> error global
        if (form.getCustomerId() != null && !result.hasFieldErrors("customerId")) {
            Customer customer = customerRepository.findById(form.getCustomerId()).orElse(null);
            if (customer == null) {
                result.rejectValue("customerId", "order.customer.notFound", "El cliente seleccionado no existe");
            } else {
                String type = form.getType();
                if ("Factura".equals(type) && !"RUC".equals(customer.getDocumentType())) {
                    result.reject("order.type.mismatch",
                            "La Factura solo puede emitirse a un cliente con RUC. El cliente seleccionado tiene "
                                    + customer.getDocumentType() + ".");
                } else if ("Boleta".equals(type) && !"DNI".equals(customer.getDocumentType())) {
                    result.reject("order.type.mismatch",
                            "La Boleta solo puede emitirse a un cliente con DNI. El cliente seleccionado tiene "
                                    + customer.getDocumentType() + ".");
                }
            }
        }
    }

    /**
     * Registra el pedido y sus detalles en una sola transacción. Los precios, subtotales y el
     * total se calculan aquí, en Java. También descuenta el stock de cada producto.
     */
    @Transactional
    public Invoice register(OrderForm form) {
        Customer customer = customerRepository.findById(form.getCustomerId())
                .orElseThrow(() -> new BusinessException("El cliente seleccionado no existe"));

        Invoice invoice = new Invoice();
        invoice.setType(form.getType());
        invoice.setDate(form.getDate());
        invoice.setCustomer(customer);

        for (OrderItemForm item : form.getItems()) {
            if (item.getProductId() == null) {
                continue;
            }
            Product product = productRepository.findByIdForUpdate(item.getProductId())
                    .orElseThrow(() -> new BusinessException("Un producto del pedido ya no existe"));

            int quantity = item.getQuantity();
            if (quantity > product.getStock()) {
                throw new BusinessException("El stock de \"" + product.getName()
                        + "\" cambió y ya no alcanza (disponible: " + product.getStock() + ")");
            }

            double subtotal = OrderMath.subtotal(quantity, product.getPrice());
            invoice.addDetail(new InvoiceDetail(product, quantity, product.getPrice(), subtotal));
            product.setStock(product.getStock() - quantity);
        }

        return invoiceRepository.save(invoice);
    }
}
