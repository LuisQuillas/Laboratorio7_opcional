package pe.edu.pucp.laboratorio7.order;

import jakarta.validation.Valid;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.edu.pucp.laboratorio7.customer.CustomerRepository;
import pe.edu.pucp.laboratorio7.product.ProductRepository;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Controller
@RequestMapping("/orders")
public class OrderController {

    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final OrderService orderService;

    public OrderController(InvoiceRepository invoiceRepository,
                           CustomerRepository customerRepository,
                           ProductRepository productRepository,
                           OrderService orderService) {
        this.invoiceRepository = invoiceRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.orderService = orderService;
    }

    @InitBinder("orderForm")
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }

    @ModelAttribute("types")
    public List<String> types() {
        return List.of("Factura", "Boleta");
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("invoices", invoiceRepository.findAllWithDetails());
        return "orders/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        OrderForm form = new OrderForm();
        form.setDate(LocalDate.now(ZoneId.of("America/Lima")));
        form.getItems().add(new OrderItemForm());
        model.addAttribute("orderForm", form);
        loadFormData(model);
        return "orders/form";
    }

    @PostMapping
    public String save(@Valid @ModelAttribute("orderForm") OrderForm form,
                       BindingResult bindingResult,
                       Model model,
                       RedirectAttributes redirectAttributes) {

        // Reglas de varios campos y de negocio (se combinan con los errores de binding/validación)
        orderService.validate(form, bindingResult);

        if (!bindingResult.hasErrors()) {
            try {
                Invoice invoice = orderService.register(form);
                redirectAttributes.addFlashAttribute("success",
                        "Pedido N.º " + invoice.getId() + " registrado correctamente");
                return "redirect:/orders/" + invoice.getId();
            } catch (BusinessException ex) {
                bindingResult.reject("order.business", ex.getMessage());
            }
        }

        // Con errores no se registra nada: se vuelve al formulario conservando cliente, tipo, fecha y líneas
        if (form.getItems().isEmpty()) {
            form.getItems().add(new OrderItemForm());
        }
        loadFormData(model);
        return "orders/form";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Integer id, Model model, RedirectAttributes redirectAttributes) {
        return invoiceRepository.findByIdWithDetails(id)
                .map(invoice -> {
                    model.addAttribute("invoice", invoice);
                    return "orders/detail";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "El pedido solicitado no existe");
                    return "redirect:/orders";
                });
    }

    private void loadFormData(Model model) {
        model.addAttribute("customers", customerRepository.findAll(Sort.by("name")));
        model.addAttribute("products", productRepository.findAll(Sort.by("name")));
    }
}
