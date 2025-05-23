package pucp.pdds.backend.seeder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import pucp.pdds.backend.model.Incidencia;
import pucp.pdds.backend.model.Incidencia.Turno;
import pucp.pdds.backend.model.Vehiculo;
import pucp.pdds.backend.repository.IncidenciaRepository;
import pucp.pdds.backend.repository.VehiculoRepository;
import java.time.LocalDate;
import java.util.List;

@Component
public class IncidenciaSeeder implements CommandLineRunner {
    
    @Autowired
    private IncidenciaRepository incidenciaRepository;
    
    @Autowired
    private VehiculoRepository vehiculoRepository;
    
    @Override
    public void run(String... args) throws Exception {
        // Get all vehicles
        List<Vehiculo> vehiculos = vehiculoRepository.findAll();
        
        if (vehiculos.isEmpty()) {
            System.out.println("No vehicles found to seed incidencias!");
            return;
        }
        
        // Create test incidencias for each vehicle
        for (Vehiculo vehiculo : vehiculos) {
            // Create an incident for each turn
            for (Turno turno : Turno.values()) {
                // Create an incident for each day of the week
                for (int day = 1; day <= 7; day++) {
                    LocalDate fecha = LocalDate.now().minusDays(day);
                    
                    // Randomly decide if the incident occurred
                    boolean ocurrido = Math.random() < 0.3; // 30% chance of occurrence
                    
                    Incidencia incidencia = new Incidencia(
                        fecha,
                        turno,
                        vehiculo,
                        ocurrido
                    );
                    
                    incidenciaRepository.save(incidencia);
                }
            }
        }
        
        System.out.println("Incidencias seeded successfully! Created test incidencias for each vehicle.");
    }
}
