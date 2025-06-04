package pucp.pdds.backend.controller;

import java.util.Date;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import pucp.pdds.backend.model.Message;
import pucp.pdds.backend.service.SimulacionSenderService;

@Controller
public class SimulacionWebSocketController {

    @MessageMapping("/chat")
    @SendTo("/topic/messages")
    public Message sendMessage(@Payload Message message){
        message.setTimestamp(new Date());
        return message;
    }

    @Autowired
    private SimulacionSenderService simulacionSenderService;

    // Cuando el frontend envíe un mensaje a /app/simulacion-start
    @MessageMapping("/simulacion-start")
    public void iniciarSimulacion(@Payload Map<String, Object> payload)  {
        System.out.println("✅ Mensaje recibido desde frontend:");
        System.out.println(payload);

        // Si quieres, también puedes acceder al campo `timestamp`
        if (payload.containsKey("timestamp")) {
            System.out.println("⏱ Fecha de inicio recibida: " + payload.get("timestamp"));
        }

        // Llamar al método que empieza la simulación por WebSocket
        simulacionSenderService.enviarSimulacionPorBatches();
    }
    @MessageMapping("/simulacion-start")
    public void test()  {
        simulacionSenderService.enviarTest();
    }

}
