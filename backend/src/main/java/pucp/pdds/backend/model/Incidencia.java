package pucp.pdds.backend.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.Map;

@Entity
@Table(name = "incidencia")
public class Incidencia {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate fecha;
    
    @Enumerated(EnumType.STRING)
    private Turno turno;
    
    @ManyToOne
    @JoinColumn(name = "vehiculo_id")
    private Vehiculo vehiculo;
    
    private boolean ocurrido;

    public enum Turno {
        T1, T2, T3
    }

    public Incidencia() {}
    
    public Incidencia(LocalDate fecha, Turno turno, Vehiculo vehiculo, boolean ocurrido) {
        this.fecha = fecha;
        this.turno = turno;
        this.vehiculo = vehiculo;
        this.ocurrido = ocurrido;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public Turno getTurno() {
        return turno;
    }

    public void setTurno(Turno turno) {
        this.turno = turno;
    }

    public Vehiculo getVehiculo() {
        return vehiculo;
    }

    public void setVehiculo(Vehiculo vehiculo) {
        this.vehiculo = vehiculo;
    }

    public boolean isOcurrido() {
        return ocurrido;
    }

    public void setOcurrido(boolean ocurrido) {
        this.ocurrido = ocurrido;
    }

    public Map<String, Object> toMap() {
        return Map.of(
            "id", id,
            "fecha", fecha.toString(),
            "turno", turno.toString(),
            "vehiculo", vehiculo != null ? vehiculo.toMap() : null,
            "ocurrido", ocurrido
        );
    }
}
