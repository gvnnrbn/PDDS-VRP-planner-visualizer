package pucp.pdds.backend.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "pedido")
public class Pedido {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String codigoCliente;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime fechaRegistro;
    
    private int posicionX;
    private int posicionY;
    private int cantidadGLP;
    private int tiempoTolerancia;

    public Pedido() {}
    
    public Pedido(String codigoCliente, LocalDateTime fechaRegistro, int posicionX, int posicionY, 
                int cantidadGLP, int tiempoTolerancia) {
        this.codigoCliente = codigoCliente;
        this.fechaRegistro = fechaRegistro;
        this.posicionX = posicionX;
        this.posicionY = posicionY;
        this.cantidadGLP = cantidadGLP;
        this.tiempoTolerancia = tiempoTolerancia;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCodigoCliente() {
        return codigoCliente;
    }

    public void setCodigoCliente(String codigoCliente) {
        this.codigoCliente = codigoCliente;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public int getPosicionX() {
        return posicionX;
    }

    public void setPosicionX(int posicionX) {
        this.posicionX = posicionX;
    }

    public int getPosicionY() {
        return posicionY;
    }

    public void setPosicionY(int posicionY) {
        this.posicionY = posicionY;
    }

    public int getCantidadGLP() {
        return cantidadGLP;
    }

    public void setCantidadGLP(int cantidadGLP) {
        this.cantidadGLP = cantidadGLP;
    }

    public int getTiempoTolerancia() {
        return tiempoTolerancia;
    }

    public void setTiempoTolerancia(int tiempoTolerancia) {
        this.tiempoTolerancia = tiempoTolerancia;
    }

    public Map<String, Object> toMap() {
        return Map.of(
            "id", id,
            "codigoCliente", codigoCliente,
            "fechaRegistro", fechaRegistro,
            "posicionX", posicionX,
            "posicionY", posicionY,
            "cantidadGLP", cantidadGLP,
            "tiempoTolerancia", tiempoTolerancia
        );
    }
}
