package pe.edu.pucp.laboratorio7.product;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void registraProductoValido() throws Exception {
        mockMvc.perform(post("/products")
                        .param("name", "Arroz").param("price", "24.90").param("stock", "10"))
                .andExpect(status().is3xxRedirection());
        assertEquals(1, productRepository.count());
    }

    @Test
    void letrasEnPrecioYStockNoSeGuardanYMuestranMensajePersonalizado() throws Exception {
        mockMvc.perform(post("/products")
                        .param("name", "Arroz").param("price", "abc").param("stock", "xyz"))
                .andExpect(status().isOk())
                .andExpect(view().name("products/form"))
                .andExpect(model().attributeHasFieldErrors("product", "price", "stock"))
                .andExpect(content().string(containsString("El precio debe ser un número válido")))
                .andExpect(content().string(containsString("El stock debe ser un número entero válido")))
                .andExpect(content().string(containsString("value=\"Arroz\"")));
        assertEquals(0, productRepository.count());
    }

    @Test
    void precioDebeSerMayorQueCeroYStockNoNegativo() throws Exception {
        mockMvc.perform(post("/products")
                        .param("name", "Arroz").param("price", "0").param("stock", "-1"))
                .andExpect(model().attributeHasFieldErrors("product", "price", "stock"));
        assertEquals(0, productRepository.count());
    }

    @Test
    void camposObligatorios() throws Exception {
        mockMvc.perform(post("/products")
                        .param("name", "").param("price", "").param("stock", ""))
                .andExpect(model().attributeHasFieldErrors("product", "name", "price", "stock"));
    }

    @Test
    void stockCeroEsValido() throws Exception {
        mockMvc.perform(post("/products")
                        .param("name", "Agotado").param("price", "1.5").param("stock", "0"))
                .andExpect(status().is3xxRedirection());
        assertEquals(1, productRepository.count());
    }

    @Test
    void nombreDuplicadoNoSeGuarda() throws Exception {
        mockMvc.perform(post("/products").param("name", "Arroz").param("price", "5").param("stock", "1"));
        mockMvc.perform(post("/products").param("name", "arroz").param("price", "6").param("stock", "2"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("product", "name"));
        assertEquals(1, productRepository.count());
    }
}
