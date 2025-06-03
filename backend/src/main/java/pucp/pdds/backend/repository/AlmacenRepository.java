package pucp.pdds.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import pucp.pdds.backend.model.Almacen;

public interface AlmacenRepository extends JpaRepository<Almacen, Long> {
}
