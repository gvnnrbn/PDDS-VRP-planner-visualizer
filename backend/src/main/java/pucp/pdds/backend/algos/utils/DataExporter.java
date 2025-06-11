package pucp.pdds.backend.algos.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;

import pucp.pdds.backend.algos.data.DataChunk;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class DataExporter {
    public static void clearSimulationData() {
        File directory = new File("simulation_data");
        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        file.delete();
                    }
                }
            }
        }
    }

    public static void exportToJson(DataChunk dataChunk, int sequence) {
        try {
            // Calculate sequential hour based on minutes since start
            int sequentialHour = sequence;
            
            String filename = "simulation_data/simulation_" + 
                sequentialHour + ".json";
            
            FileWriter fileWriter = new FileWriter(filename);
            
            Gson gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new DataChunk.LocalDateTimeAdapter())
                .setPrettyPrinting()
                .create();
            
            fileWriter.write(gson.toJson(dataChunk));
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
} 