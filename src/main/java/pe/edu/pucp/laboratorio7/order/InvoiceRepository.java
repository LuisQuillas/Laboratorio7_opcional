package pe.edu.pucp.laboratorio7.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Integer> {

    @Query("select distinct i from Invoice i "
            + "join fetch i.customer "
            + "left join fetch i.details d "
            + "left join fetch d.product "
            + "order by i.id desc")
    List<Invoice> findAllWithDetails();

    @Query("select i from Invoice i "
            + "join fetch i.customer "
            + "left join fetch i.details d "
            + "left join fetch d.product "
            + "where i.id = :id")
    Optional<Invoice> findByIdWithDetails(@Param("id") Integer id);
}
