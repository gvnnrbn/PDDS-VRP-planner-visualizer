package utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import data.DataChunk;

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

    public static void exportToJson(DataChunk dataChunk, Time currentTime) {
        try {
            String filename = "simulation_data/simulation_" + 
                currentTime.getYear() + "_" + 
                currentTime.getMonth() + "_" + 
                currentTime.getDay() + "_" + 
                currentTime.getHour() + "_" + 
                currentTime.getMinute() + ".json";
            
            FileWriter fileWriter = new FileWriter(filename);
            
            fileWriter.write("{\n");
            fileWriter.write("  \"minutes\": [\n");
            
            for (int minuteIndex = 0; minuteIndex < dataChunk.minutes.size(); minuteIndex++) {
                DataChunk.MinuteData minute = dataChunk.minutes.get(minuteIndex);
                fileWriter.write("    {\n");
                fileWriter.write("      \"currTime\": \"" + minute.currTime.toString() + "\",\n");
                fileWriter.write("      \"vehicles\": [\n");
                
                for (int j = 0; j < minute.vehicles.size(); j++) {
                    DataChunk.VehicleData vehicle = minute.vehicles.get(j);
                    fileWriter.write("        {\n");
                    fileWriter.write("          \"plaque\": \"" + vehicle.plaque + "\",\n");
                    fileWriter.write("          \"position\": {\n");
                    fileWriter.write("            \"x\": " + vehicle.position.x + ",\n");
                    fileWriter.write("            \"y\": " + vehicle.position.y + "\n");
                    fileWriter.write("          },\n");
                    fileWriter.write("          \"state\": \"" + vehicle.state.toString() + "\"\n");
                    fileWriter.write("        }" + (j < minute.vehicles.size() - 1 ? "," : "") + "\n");
                }
                
                fileWriter.write("      ]\n");
                fileWriter.write("    }" + (minuteIndex < dataChunk.minutes.size() - 1 ? "," : "") + "\n");
            }
            
            fileWriter.write("  ]\n");
            fileWriter.write("}\n");
            
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
} 