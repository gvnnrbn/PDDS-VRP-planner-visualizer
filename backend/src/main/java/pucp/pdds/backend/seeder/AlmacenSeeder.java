package pucp.pdds.backend.seeder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import pucp.pdds.backend.model.Almacen;
import pucp.pdds.backend.repository.AlmacenRepository;

@Component
public class AlmacenSeeder implements CommandLineRunner {

    @Autowired
    private AlmacenRepository almacenRepository;

    @Override
    public void run(String... args) throws Exception {
        almacenRepository.deleteAll();

        // Almacén Principal (capacidad infinita simulada con Float.MAX_VALUE)
        Almacen principal = new Almacen();
        principal.setEsPrincipal(true);
        principal.setCapacidadEfectivam3(Float.MAX_VALUE);
        principal.setHorarioAbastecimiento("Siempre");
        principal.setPosicionX(12);
        principal.setPosicionY(8);
        almacenRepository.save(principal);

        // Almacén Intermedio Norte
        Almacen intermedioNorte = new Almacen();
        intermedioNorte.setEsPrincipal(false);
        intermedioNorte.setCapacidadEfectivam3(160.0f);
        intermedioNorte.setHorarioAbastecimiento("00:00");
        intermedioNorte.setPosicionX(42);
        intermedioNorte.setPosicionY(42);
        almacenRepository.save(intermedioNorte);

        // Almacén Intermedio Este
        Almacen intermedioEste = new Almacen();
        intermedioEste.setEsPrincipal(false);
        intermedioEste.setCapacidadEfectivam3(160.0f);
        intermedioEste.setHorarioAbastecimiento("00:00");
        intermedioEste.setPosicionX(63);
        intermedioEste.setPosicionY(3);
        almacenRepository.save(intermedioEste);

        System.out.println("Almacenes seeded successfully! 1 principal + 2 intermedios.");
    }
}
