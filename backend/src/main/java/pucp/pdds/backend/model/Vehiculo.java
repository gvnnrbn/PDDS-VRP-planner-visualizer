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

    public enum TipoVehiculo {
        TA, TB, TC, TD
    }
    private int peso;
    private float maxCombustible;
    private float maxGlp;
    private float currCombustible;
    private float currGlp;
    private float posicionX;
    private float posicionY;
    private boolean disponible;

    public Vehiculo() {}
    
    public Vehiculo(TipoVehiculo tipo, int peso, float maxCombustible, float maxGlp) {
        this.tipo = tipo;
        this.peso = peso;
        this.maxCombustible = maxCombustible;
        this.maxGlp = maxGlp;
        this.currCombustible = maxCombustible; // Start with full tank
        this.currGlp = maxGlp; // Start with full tank
        this.posicionX = 0.0f; // Start at origin
        this.posicionY = 0.0f; // Start at origin
        this.disponible = true;
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

    public int getPeso() {
        return peso;
    }

    public void setPeso(int peso) {
        this.peso = peso;
    }

    public float getMaxCombustible() {
        return maxCombustible;
    }

    public void setMaxCombustible(float maxCombustible) {
        this.maxCombustible = maxCombustible;
    }

    public float getMaxGlp() {
        return maxGlp;
    }

    public void setMaxGlp(float maxGlp) {
        this.maxGlp = maxGlp;
    }

    public float getCurrCombustible() {
        return currCombustible;
    }

    public void setCurrCombustible(float currCombustible) {
        this.currCombustible = currCombustible;
    }

    public float getCurrGlp() {
        return currGlp;
    }

    public void setCurrGlp(float currGlp) {
        this.currGlp = currGlp;
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

    public boolean isDisponible() {
        return disponible;
    }

    public void setDisponible(boolean disponible) {
        this.disponible = disponible;
    }

    public Map<String, Object> toMap() {
        return Map.of(
            "id", id,
            "tipo", tipo,
            "peso", peso,
            "maxCombustible", maxCombustible,
            "maxGlp", maxGlp,
            "currCombustible", currCombustible,
            "currGlp", currGlp,
            "posicionX", posicionX,
            "posicionY", posicionY,
            "disponible", disponible
        );
    }
}
