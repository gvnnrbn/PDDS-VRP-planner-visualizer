package pucp.pdds.backend.controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

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
        simulationService.isSimulationActive();
    }

    @MessageMapping("/update-failures")
    public void updateFailures(UpdateFailuresMessage message) {
        System.out.println("failure request message: " + message);
        System.out.println("Received failure simulation request");
        simulationService.updateFailures(message);

    }

    @MessageMapping("/stop")
    public void stopSimulation() {
        simulationService.stopSimulation();
    }

    // @MessageMapping("/status")
    // public void getSimulationStatus() {
    //     simulationService.isSimulationActive();
    // }
}
