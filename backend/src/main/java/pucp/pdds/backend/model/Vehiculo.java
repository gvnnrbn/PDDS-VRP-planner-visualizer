package pucp.pdds.backend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Map;

@Entity
@Table(name = "vehiculo")
public class Vehiculo {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private TipoVehiculo tipo;

    public Vehiculo() {}
    
    public Vehiculo(TipoVehiculo tipo) {
        this.tipo = tipo;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TipoVehiculo getTipo() {
        return tipo;
    }

    public void setTipo(TipoVehiculo tipo) {
        this.tipo = tipo;
    }

    public Map<String, Object> toMap() {
        return Map.of(
            "id", id,
            "tipo", Map.of(
                "name", tipo.name(),
                "peso", tipo.getPeso(),
                "maxCombustible", tipo.getMaxCombustible(),
                "maxGlp", tipo.getMaxGlp()
            )
        );
    }
}
