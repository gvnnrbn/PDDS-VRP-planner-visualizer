package pucp.pdds.backend.seeder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import pucp.pdds.backend.model.Vehiculo;
import pucp.pdds.backend.model.Vehiculo.TipoVehiculo;
import pucp.pdds.backend.repository.VehiculoRepository;

@Component
public class VehiculoSeeder implements CommandLineRunner {
    
    @Autowired
    private VehiculoRepository vehiculoRepository;
    
    @Override
    public void run(String... args) throws Exception {
        vehiculoRepository.deleteAll();
        
        // Vehicle types with their specifications
        String[][] vehiculos = {
            {"TA", "1000", "50", "20"},
            {"TB", "1500", "60", "30"},
            {"TC", "2000", "70", "40"},
            {"TD", "2500", "80", "50"}
        };
        
        for (String[] vehiculoData : vehiculos) {
            Vehiculo vehiculo = new Vehiculo(
                TipoVehiculo.valueOf(vehiculoData[0]),
                Integer.parseInt(vehiculoData[1]),
                Float.parseFloat(vehiculoData[2]),
                Float.parseFloat(vehiculoData[3])
            );
            vehiculoRepository.save(vehiculo);
        }
        
        System.out.println("Vehiculos seeded successfully! Created one vehicle of each tipo.");
    }
}
