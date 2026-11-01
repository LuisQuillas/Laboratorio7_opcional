package pe.edu.pucp.laboratorio7.order;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.laboratorio7.customer.Customer;
import pe.edu.pucp.laboratorio7.customer.CustomerRepository;
import pe.edu.pucp.laboratorio7.product.Product;
import pe.edu.pucp.laboratorio7.product.ProductRepository;

import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private InvoiceRepository invoiceRepository;
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private ProductRepository productRepository;

    private Customer dniCustomer;
    private Customer rucCustomer;
    private Product rice;
    private Product oil;

    @BeforeEach
    void setUp() {
        dniCustomer = saveCustomer("Juan Pérez", "DNI", "12345678");
        rucCustomer = saveCustomer("Comercial Andina", "RUC", "20123456789");
        rice = saveProduct("Arroz", 19.99, 10);
        oil = saveProduct("Aceite", 9.5, 5);
    }

    private Customer saveCustomer(String name, String type, String document) {
        Customer c = new Customer();
        c.setName(name);
        c.setDocumentType(type);
        c.setDocument(document);
        return customerRepository.save(c);
    }

    private Product saveProduct(String name, double price, int stock) {
        Product p = new Product();
        p.setName(name);
        p.setPrice(price);
        p.setStock(stock);
        return productRepository.save(p);
    }

    @Test
    void facturaARucSeRegistraConSubtotalesYTotalCalculadosEnJava() throws Exception {
        mockMvc.perform(post("/orders")
                        .param("type", "Factura")
                        .param("date", LocalDate.now().toString())
                        .param("customerId", String.valueOf(rucCustomer.getId()))
                        .param("items[0].productId", String.valueOf(rice.getId()))
                        .param("items[0].quantity", "3")
                        .param("items[1].productId", String.valueOf(oil.getId()))
                        .param("items[1].quantity", "2"))
                .andExpect(status().is3xxRedirection());

        Invoice invoice = invoiceRepository.findAllWithDetails().get(0);
        assertEquals(2, invoice.getDetails().size());
        // 3 x 19.99 = 59.97 ; 2 x 9.50 = 19.00 ; total = 78.97
        assertEquals(78.97, invoice.getTotal(), 0.0001);
        assertEquals(59.97, invoice.getDetails().stream()
                .filter(d -> d.getProduct().getName().equals("Arroz")).findFirst().orElseThrow().getSubtotal(), 0.0001);
        assertEquals(7, productRepository.findById(rice.getId()).orElseThrow().getStock());
    }

    @Test
    void boletaARucMuestraErrorGlobalYNoRegistra() throws Exception {
        mockMvc.perform(post("/orders")
                        .param("type", "Boleta")
                        .param("date", LocalDate.now().toString())
                        .param("customerId", String.valueOf(rucCustomer.getId()))
                        .param("items[0].productId", String.valueOf(rice.getId()))
                        .param("items[0].quantity", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("orders/form"))
                .andExpect(model().attributeHasErrors("orderForm"))
                .andExpect(content().string(containsString("La Boleta solo puede emitirse a un cliente con DNI")));
        assertEquals(0, invoiceRepository.count());
    }

    @Test
    void facturaADniMuestraErrorGlobal() throws Exception {
        mockMvc.perform(post("/orders")
                        .param("type", "Factura")
                        .param("date", LocalDate.now().toString())
                        .param("customerId", String.valueOf(dniCustomer.getId()))
                        .param("items[0].productId", String.valueOf(rice.getId()))
                        .param("items[0].quantity", "1"))
                .andExpect(content().string(containsString("La Factura solo puede emitirse a un cliente con RUC")));
        assertEquals(0, invoiceRepository.count());
    }

    @Test
    void cantidadMayorAlStockSeRechaza() throws Exception {
        mockMvc.perform(post("/orders")
                        .param("type", "Boleta")
                        .param("date", LocalDate.now().toString())
                        .param("customerId", String.valueOf(dniCustomer.getId()))
                        .param("items[0].productId", String.valueOf(oil.getId()))
                        .param("items[0].quantity", "6"))
                .andExpect(model().attributeHasFieldErrors("orderForm", "items[0].quantity"));
        assertEquals(0, invoiceRepository.count());
        assertEquals(5, productRepository.findById(oil.getId()).orElseThrow().getStock());
    }

    @Test
    void cantidadCeroONegativaSeRechaza() throws Exception {
        mockMvc.perform(post("/orders")
                        .param("type", "Boleta")
                        .param("date", LocalDate.now().toString())
                        .param("customerId", String.valueOf(dniCustomer.getId()))
                        .param("items[0].productId", String.valueOf(oil.getId()))
                        .param("items[0].quantity", "0"))
                .andExpect(model().attributeHasFieldErrors("orderForm", "items[0].quantity"));
    }

    @Test
    void productoRepetidoSeRechaza() throws Exception {
        mockMvc.perform(post("/orders")
                        .param("type", "Boleta")
                        .param("date", LocalDate.now().toString())
                        .param("customerId", String.valueOf(dniCustomer.getId()))
                        .param("items[0].productId", String.valueOf(rice.getId()))
                        .param("items[0].quantity", "1")
                        .param("items[1].productId", String.valueOf(rice.getId()))
                        .param("items[1].quantity", "2"))
                .andExpect(model().attributeHasFieldErrors("orderForm", "items[1].productId"));
        assertEquals(0, invoiceRepository.count());
    }

    @Test
    void fechaFuturaSeRechaza() throws Exception {
        mockMvc.perform(post("/orders")
                        .param("type", "Boleta")
                        .param("date", LocalDate.now().plusDays(3).toString())
                        .param("customerId", String.valueOf(dniCustomer.getId()))
                        .param("items[0].productId", String.valueOf(rice.getId()))
                        .param("items[0].quantity", "1"))
                .andExpect(model().attributeHasFieldErrors("orderForm", "date"));
        assertEquals(0, invoiceRepository.count());
    }

    @Test
    void sinProductosSeRechaza() throws Exception {
        mockMvc.perform(post("/orders")
                        .param("type", "Boleta")
                        .param("date", LocalDate.now().toString())
                        .param("customerId", String.valueOf(dniCustomer.getId())))
                .andExpect(model().attributeHasFieldErrors("orderForm", "items"));
        assertEquals(0, invoiceRepository.count());
    }

    @Test
    void letrasEnLaCantidadNoRegistranYConservanLaSeleccion() throws Exception {
        mockMvc.perform(post("/orders")
                        .param("type", "Boleta")
                        .param("date", LocalDate.now().toString())
                        .param("customerId", String.valueOf(dniCustomer.getId()))
                        .param("items[0].productId", String.valueOf(rice.getId()))
                        .param("items[0].quantity", "abc"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("orderForm", "items[0].quantity"))
                .andExpect(model().attribute("orderForm",
                        org.hamcrest.Matchers.hasProperty("customerId",
                                org.hamcrest.Matchers.is(dniCustomer.getId()))));
        assertEquals(0, invoiceRepository.count());
    }

    @Test
    void clienteYTipoSonObligatorios() throws Exception {
        mockMvc.perform(post("/orders")
                        .param("type", "")
                        .param("date", LocalDate.now().toString())
                        .param("items[0].productId", String.valueOf(rice.getId()))
                        .param("items[0].quantity", "1"))
                .andExpect(model().attributeHasFieldErrors("orderForm", "type", "customerId"));
    }

    @Test
    void listadoYFormularioSeMuestran() throws Exception {
        mockMvc.perform(get("/orders")).andExpect(status().isOk()).andExpect(view().name("orders/list"));
        mockMvc.perform(get("/orders/new")).andExpect(status().isOk()).andExpect(view().name("orders/form"));
    }
}
