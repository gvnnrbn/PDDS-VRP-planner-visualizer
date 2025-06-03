package pucp.pdds.backend.model;

import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "vehiculo")
public class Vehiculo {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Enumerated(EnumType.STRING)
    private TipoVehiculo tipo;

    public enum TipoVehiculo {
        TA, TB, TC, TD
    }

    @Column(name = "peso")
    private int peso;

    @Column(name = "max_combustible")
    private float maxCombustible;

    @Column(name = "max_glp")
    private float maxGlp;

    @Column(name = "curr_combustible")
    private float currCombustible;

    @Column(name = "curr_glp")
    private float currGlp;

    @Column(name = "posicionx")
    private float posicionX;

    @Column(name = "posiciony")
    private float posicionY;

    @Column(name = "disponible")
    private boolean disponible;

    @Column(name = "placa", nullable = true, unique = true)
    private String placa;

    public Vehiculo() {}
    
    public Vehiculo(TipoVehiculo tipo, String placa, int peso, float maxCombustible, float maxGlp) {
        this.tipo = tipo;
        this.peso = peso;
        this.maxCombustible = maxCombustible;
        this.maxGlp = maxGlp;
        this.currCombustible = maxCombustible; // Start with full tank
        this.currGlp = maxGlp; // Start with full tank
        this.posicionX = 0.0f; // Start at origin
        this.posicionY = 0.0f; // Start at origin
        this.disponible = true;
        this.placa = placa;
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
    public String getPlaca() {
        return placa;
    }

    public void setPlaca(String placa) {
        this.placa = placa;
    }


    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("tipo", tipo);
        map.put("placa", placa);
        map.put("peso", peso);
        map.put("maxCombustible", maxCombustible);
        map.put("maxGlp", maxGlp);
        map.put("currCombustible", currCombustible);
        map.put("currGlp", currGlp);
        map.put("posicionX", posicionX);
        map.put("posicionY", posicionY);
        map.put("disponible", disponible);
        return map;
    }
}
