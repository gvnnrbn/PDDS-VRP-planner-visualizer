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
            {"TA","CJ2111" ,"1000", "50", "20"},
            {"TB", "RRR222", "800","60", "30"},
            {"TC", "SS3333","1000", "70", "40"},
            {"TD", "TUUU11", "800","80", "50"}
        };
        
        for (String[] vehiculoData : vehiculos) {
            Vehiculo vehiculo = new Vehiculo(
                TipoVehiculo.valueOf(vehiculoData[0]),
                String.valueOf(vehiculoData[1]),
                Integer.parseInt(vehiculoData[2]),
                Float.parseFloat(vehiculoData[3]),
                Float.parseFloat(vehiculoData[4])
            );
            vehiculoRepository.save(vehiculo);
        }
        
        System.out.println("Vehiculos seeded successfully! Created one vehicle of each tipo.");
    }
}
