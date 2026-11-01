package pe.edu.pucp.laboratorio7.customer;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Integer> {

    boolean existsByDocument(String document);

    boolean existsByDocumentAndIdNot(String document, Integer id);
}
