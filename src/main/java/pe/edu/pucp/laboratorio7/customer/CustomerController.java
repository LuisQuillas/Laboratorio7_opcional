package pe.edu.pucp.laboratorio7.customer;

import jakarta.validation.Valid;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.dao.DataIntegrityViolationException;
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

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/customers")
public class CustomerController {

    private final CustomerRepository customerRepository;

    public CustomerController(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    /** Quita espacios al inicio/fin; un texto vacío queda null y lo reporta @NotBlank. */
    @InitBinder("customer")
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }

    @ModelAttribute("documentTypes")
    public List<String> documentTypes() {
        return List.of("DNI", "RUC");
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("customers", customerRepository.findAll(Sort.by("id")));
        return "customers/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("customer", new Customer());
        return "customers/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Integer id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Customer> customer = customerRepository.findById(id);
        if (customer.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "El cliente solicitado no existe");
            return "redirect:/customers";
        }
        model.addAttribute("customer", customer.get());
        return "customers/form";
    }

    /** Un único método para crear (id == null) y editar (id != null) usando la misma vista. */
    @PostMapping
    public String save(@Valid @ModelAttribute("customer") Customer customer,
                       BindingResult bindingResult,
                       RedirectAttributes redirectAttributes) {

        boolean isNew = customer.getId() == null;

        // Documento único (solo si el campo ya pasó las validaciones de formato)
        if (!bindingResult.hasFieldErrors("document") && customer.getDocument() != null) {
            boolean duplicated = isNew
                    ? customerRepository.existsByDocument(customer.getDocument())
                    : customerRepository.existsByDocumentAndIdNot(customer.getDocument(), customer.getId());
            if (duplicated) {
                bindingResult.rejectValue("document", "customer.document.duplicated",
                        "Ya existe un cliente registrado con el documento " + customer.getDocument());
            }
        }

        if (!isNew && !customerRepository.existsById(customer.getId())) {
            redirectAttributes.addFlashAttribute("error", "El cliente que intentas editar ya no existe");
            return "redirect:/customers";
        }

        if (bindingResult.hasErrors()) {
            // Se vuelve a mostrar la misma vista con los valores ingresados y los errores por campo
            return "customers/form";
        }

        try {
            customerRepository.save(customer);
        } catch (DataIntegrityViolationException ex) {
            // Respaldo ante una carrera de concurrencia con la restricción UNIQUE de la BD
            bindingResult.rejectValue("document", "customer.document.duplicated",
                    "Ya existe un cliente registrado con el documento " + customer.getDocument());
            return "customers/form";
        }

        redirectAttributes.addFlashAttribute("success",
                isNew ? "Cliente registrado correctamente" : "Cliente actualizado correctamente");
        return "redirect:/customers";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            customerRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("success", "Cliente eliminado correctamente");
        } catch (DataIntegrityViolationException ex) {
            redirectAttributes.addFlashAttribute("error",
                    "No se puede eliminar el cliente porque tiene pedidos registrados");
        } catch (org.springframework.dao.EmptyResultDataAccessException ex) {
            redirectAttributes.addFlashAttribute("error", "El cliente ya no existe");
        }
        return "redirect:/customers";
    }
}
