package pucp.pdds.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import pucp.pdds.backend.service.DailyService;

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
}
