package pucp.pdds.backend.dto.collapse;

public class CollapseSimulationResponse {
    private String type;
    private Object data;

    public CollapseSimulationResponse(String type, Object data) {
        this.type = type;
        this.data = data;
    }

    public String getType() { return type; }
    public Object getData() { return data; }
} 