package pucp.pdds.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import pucp.pdds.backend.service.CollapseService;

@Controller
public class CollapseController {

    @Autowired
    private CollapseService collapseService;

    @MessageMapping("/init-collapse")
    public void startCollapseSimulation(@Payload String fechaInicioStr) {
        collapseService.startSimulation(fechaInicioStr);
    }

    @MessageMapping("/stop-collapse")
    public void stopCollapseSimulation() {
        collapseService.stopSimulation();
    }
} 