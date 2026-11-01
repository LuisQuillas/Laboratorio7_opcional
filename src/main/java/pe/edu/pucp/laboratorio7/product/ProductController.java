package pe.edu.pucp.laboratorio7.product;

import jakarta.validation.Valid;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
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

import java.util.Optional;

@Controller
@RequestMapping("/products")
public class ProductController {

    private final ProductRepository productRepository;

    public ProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /*
     * Conversión de campos numéricos (precio y stock):
     * ALTERNATIVA IMPLEMENTADA: personalización mediante messages.properties
     * (claves typeMismatch.product.price y typeMismatch.product.stock).
     * No se usa @InitBinder para convertir números; aquí solo se recortan espacios en textos.
     *
     * Si el usuario escribe letras en precio/stock, Spring registra un error de binding (typeMismatch),
     * el campo queda sin valor, BindingResult.hasErrors() es true y el producto NO se guarda.
     */
    @InitBinder("product")
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("products", productRepository.findAll(Sort.by("id")));
        return "products/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("product", new Product());
        return "products/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Integer id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Product> product = productRepository.findById(id);
        if (product.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "El producto solicitado no existe");
            return "redirect:/products";
        }
        model.addAttribute("product", product.get());
        return "products/form";
    }

    /** Un único formulario/método para crear y editar. */
    @PostMapping
    public String save(@Valid @ModelAttribute("product") Product product,
                       BindingResult bindingResult,
                       RedirectAttributes redirectAttributes) {

        boolean isNew = product.getId() == null;

        if (!bindingResult.hasFieldErrors("name") && product.getName() != null) {
            boolean duplicated = isNew
                    ? productRepository.existsByNameIgnoreCase(product.getName())
                    : productRepository.existsByNameIgnoreCaseAndIdNot(product.getName(), product.getId());
            if (duplicated) {
                bindingResult.rejectValue("name", "product.name.duplicated",
                        "Ya existe un producto con el nombre \"" + product.getName() + "\"");
            }
        }

        if (!isNew && !productRepository.existsById(product.getId())) {
            redirectAttributes.addFlashAttribute("error", "El producto que intentas editar ya no existe");
            return "redirect:/products";
        }

        if (bindingResult.hasErrors()) {
            // Se vuelve a mostrar el formulario con los datos ingresados y los mensajes por campo
            return "products/form";
        }

        try {
            productRepository.save(product);
        } catch (DataIntegrityViolationException ex) {
            bindingResult.rejectValue("name", "product.name.duplicated",
                    "Ya existe un producto con el nombre \"" + product.getName() + "\"");
            return "products/form";
        }

        redirectAttributes.addFlashAttribute("success",
                isNew ? "Producto registrado correctamente" : "Producto actualizado correctamente");
        return "redirect:/products";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            productRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("success", "Producto eliminado correctamente");
        } catch (DataIntegrityViolationException ex) {
            redirectAttributes.addFlashAttribute("error",
                    "No se puede eliminar el producto porque forma parte de pedidos registrados");
        } catch (EmptyResultDataAccessException ex) {
            redirectAttributes.addFlashAttribute("error", "El producto ya no existe");
        }
        return "redirect:/products";
    }
}
