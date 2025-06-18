package pucp.pdds.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "bloqueo")
public class Bloqueo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String verticesJson; // JSON array of {x, y} objects

    public Bloqueo() {}

    public Bloqueo(LocalDateTime startTime, LocalDateTime endTime, String verticesJson) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.verticesJson = verticesJson;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public String getVerticesJson() { return verticesJson; }
    public void setVerticesJson(String verticesJson) { this.verticesJson = verticesJson; }
} 