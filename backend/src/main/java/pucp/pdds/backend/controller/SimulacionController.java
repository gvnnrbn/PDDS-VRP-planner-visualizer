package pucp.pdds.backend.controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import pucp.pdds.backend.service.SimulacionSenderService;

@Controller
public class SimulacionController {
    @Autowired
    private SimulacionSenderService simulacionSenderService;
    private static final DateTimeFormatter DATE_FORMATTER = 
    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    @MessageMapping("/simulacion-start")
    public void iniciarSimulacion(@Payload String fechaInicioStr) {
        System.out.println("Iniciando simulacion con fecha: " + fechaInicioStr);
        try {
            LocalDateTime fechaInicio = LocalDateTime.parse(fechaInicioStr, DATE_FORMATTER);
            simulacionSenderService.enviarSimulacionPorBatches(fechaInicio);
        } catch (DateTimeParseException e) {
            System.err.println("Error parsing date: " + fechaInicioStr);
            // Fallback to current time if parsing fails
            simulacionSenderService.enviarSimulacionPorBatches(LocalDateTime.now());
        }
    }

    @MessageMapping("/simulacion-test")
    public void testSimulacion() {
        simulacionSenderService.enviarTest(); // Empieza a enviar la simulación falsa
    }

}
