package pucp.pdds.backend.seeder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import pucp.pdds.backend.model.TipoVehiculo;
import pucp.pdds.backend.model.Vehiculo;
import pucp.pdds.backend.repository.VehiculoRepository;

import java.util.Arrays;

@Component
public class VehiculoSeeder implements CommandLineRunner {
    
    @Autowired
    private VehiculoRepository vehiculoRepository;
    
    @Override
    public void run(String... args) throws Exception {
        vehiculoRepository.deleteAll();
        
        Arrays.stream(TipoVehiculo.values())
            .forEach(tipo -> {
                Vehiculo vehiculo = new Vehiculo(tipo);
                vehiculoRepository.save(vehiculo);
            });
        
        System.out.println("Vehiculos seeded successfully! Created one vehicle of each tipo.");
    }
}
