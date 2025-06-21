package pucp.pdds.backend.controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import pucp.pdds.backend.dto.InitMessage;
import pucp.pdds.backend.dto.UpdateFailuresMessage;
import pucp.pdds.backend.service.SimulationService;

@Controller
public class SimulationController {
    @Autowired
    private SimulationService simulationService;

    @MessageMapping("/init")
    public void startSimulation(@Payload String fechaInicioStr) {
        System.out.println("Received start simulation request with date: " + fechaInicioStr);

        simulationService.startSimulation(fechaInicioStr);
    }

    @MessageMapping("/update-failures")
    public void updateFailures(UpdateFailuresMessage message) {
        simulationService.updateFailures(message);
    }

    @MessageMapping("/stop")
    public void stopSimulation() {
        simulationService.stopSimulation();
    }
}
