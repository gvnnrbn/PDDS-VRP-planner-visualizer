package pucp.pdds.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import pucp.pdds.backend.service.DailyService;
import pucp.pdds.backend.dto.UpdateFailuresMessage;

@Controller
public class DailyController {
    
    @Autowired
    private DailyService dailyService;

    @MessageMapping("/daily/update")
    public void refetchData() {
        dailyService.refetchData();
    }

    @MessageMapping("/daily/fetch")
    public void fetchData() {
        dailyService.fetchData();
    }

    @MessageMapping("/daily/update-failures")
    public void updateFailures(UpdateFailuresMessage message) {
        System.out.println("[DAILY CONTROLLER] Received failure request message: " + message);
        System.out.println("[DAILY CONTROLLER] Vehicle: " + message.getVehiclePlaque() + ", Type: " + message.getType() + ", Shift: " + message.getShiftOccurredOn());
        System.out.println("[DAILY CONTROLLER] Processing failure daily operation request");
        dailyService.updateFailures(message);
    }
}
