package pe.edu.pucp.laboratorio7.customer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    void registraClienteValido() throws Exception {
        mockMvc.perform(post("/customers")
                        .param("name", "Ana Torres")
                        .param("documentType", "DNI")
                        .param("document", "12345678"))
                .andExpect(status().is3xxRedirection());
        assertEquals(1, customerRepository.count());
    }

    @Test
    void dniConMenosDe8DigitosMuestraErrorYConservaValores() throws Exception {
        mockMvc.perform(post("/customers")
                        .param("name", "Ana Torres")
                        .param("documentType", "DNI")
                        .param("document", "1234567"))
                .andExpect(status().isOk())
                .andExpect(view().name("customers/form"))
                .andExpect(model().attributeHasFieldErrors("customer", "document"))
                .andExpect(model().attribute("customer",
                        org.hamcrest.Matchers.hasProperty("name", org.hamcrest.Matchers.is("Ana Torres"))));
        assertEquals(0, customerRepository.count());
    }

    @Test
    void rucDebeTener11Digitos() throws Exception {
        mockMvc.perform(post("/customers")
                        .param("name", "Empresa SAC")
                        .param("documentType", "RUC")
                        .param("document", "12345678"))
                .andExpect(model().attributeHasFieldErrors("customer", "document"));
        assertEquals(0, customerRepository.count());
    }

    @Test
    void nombreYTipoSonObligatorios() throws Exception {
        mockMvc.perform(post("/customers")
                        .param("name", "")
                        .param("documentType", "")
                        .param("document", "12345678"))
                .andExpect(model().attributeHasFieldErrors("customer", "name", "documentType"));
        assertEquals(0, customerRepository.count());
    }

    @Test
    void tipoDeDocumentoSoloDniORuc() throws Exception {
        mockMvc.perform(post("/customers")
                        .param("name", "Ana")
                        .param("documentType", "PASAPORTE")
                        .param("document", "12345678"))
                .andExpect(model().attributeHasFieldErrors("customer", "documentType"));
    }

    @Test
    void documentoDuplicadoNoSeGuarda() throws Exception {
        mockMvc.perform(post("/customers")
                .param("name", "Ana Torres").param("documentType", "DNI").param("document", "12345678"));

        mockMvc.perform(post("/customers")
                        .param("name", "Otra Persona")
                        .param("documentType", "DNI")
                        .param("document", "12345678"))
                .andExpect(status().isOk())
                .andExpect(view().name("customers/form"))
                .andExpect(model().attributeHasFieldErrors("customer", "document"));
        assertEquals(1, customerRepository.count());
    }

    @Test
    void editarUsaLaMismaVistaYNoMarcaDuplicadoConsigoMismo() throws Exception {
        Customer c = new Customer();
        c.setName("Ana Torres");
        c.setDocumentType("DNI");
        c.setDocument("12345678");
        c = customerRepository.save(c);

        mockMvc.perform(post("/customers")
                        .param("id", String.valueOf(c.getId()))
                        .param("name", "Ana Torres Díaz")
                        .param("documentType", "DNI")
                        .param("document", "12345678"))
                .andExpect(status().is3xxRedirection());
        assertEquals("Ana Torres Díaz", customerRepository.findById(c.getId()).orElseThrow().getName());
    }
}
