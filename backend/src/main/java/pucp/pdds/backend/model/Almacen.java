package pucp.pdds.backend.model;

import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "almacen")
public class Almacen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "posicionx")
    private float posicionX;
    
    @Column(name = "posiciony")
    private float posicionY;

    @Column(name = "capacidad_efectivam3")
    private float capacidadEfectivam3;

    @Column(name = "es_principal")
    private boolean esPrincipal;

    @Column(name = "horario_abastecimiento")
    private String horarioAbastecimiento;

    public Almacen() {
        // Por defecto: intermedio
        this.esPrincipal = false;
        this.capacidadEfectivam3 = 160;
        this.horarioAbastecimiento = "00:00";
    }

    public Almacen(float posicionX, float posicionY, boolean esPrincipal) {
        this.posicionX = posicionX;
        this.posicionY = posicionY;
        this.setEsPrincipal(esPrincipal); // esto asigna correctamente los valores por tipo
    }

    // Getters y Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public float getPosicionX() {
        return posicionX;
    }

    public void setPosicionX(float posicionX) {
        this.posicionX = posicionX;
    }

    public float getPosicionY() {
        return posicionY;
    }

    public void setPosicionY(float posicionY) {
        this.posicionY = posicionY;
    }

    public float getCapacidadEfectivam3() {
        return capacidadEfectivam3;
    }

    public void setCapacidadEfectivam3(float capacidadEfectivam3) {
        this.capacidadEfectivam3 = capacidadEfectivam3;
    }

    public boolean isEsPrincipal() {
        return esPrincipal;
    }

    public void setEsPrincipal(boolean esPrincipal) {
        this.esPrincipal = esPrincipal;
        if (esPrincipal) {
            this.capacidadEfectivam3 = Float.MAX_VALUE;
            this.horarioAbastecimiento = "Siempre";
        } else {
            this.capacidadEfectivam3 = 160;
            this.horarioAbastecimiento = "00:00";
        }
    }

    public String getHorarioAbastecimiento() {
        return horarioAbastecimiento;
    }

    public void setHorarioAbastecimiento(String horarioAbastecimiento) {
        this.horarioAbastecimiento = horarioAbastecimiento;
    }

    public Map<String, Object> toMap() {
        return Map.of(
            "id", id,
            "posicionX", posicionX,
            "posicionY", posicionY,
            "capacidadEfectivam3", capacidadEfectivam3,
            "esPrincipal", esPrincipal,
            "horarioAbastecimiento", horarioAbastecimiento
        );
    }
}
