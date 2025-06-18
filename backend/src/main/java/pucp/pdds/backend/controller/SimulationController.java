package pucp.pdds.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import pucp.pdds.backend.dto.InitMessage;
import pucp.pdds.backend.dto.UpdateFailuresMessage;
import pucp.pdds.backend.service.SimulationService;

@Controller
public class SimulationController {
    @Autowired
    private SimulationService simulationService;

    @MessageMapping("/init")
    public void startSimulation(InitMessage message) {
        simulationService.startSimulation(message);
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
