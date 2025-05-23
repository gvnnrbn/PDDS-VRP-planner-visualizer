package pucp.pdds.backend.dto;

public class TipoVehiculoDto {
    private String tipo;
    private int peso;
    private int maxCombustible;
    private int maxGlp;

    public TipoVehiculoDto(String tipo, int peso, int maxCombustible, int maxGlp) {
        this.tipo = tipo;
        this.peso = peso;
        this.maxCombustible = maxCombustible;
        this.maxGlp = maxGlp;
    }

    public String getTipo() {
        return tipo;
    }

    public int getPeso() {
        return peso;
    }

    public int getMaxCombustible() {
        return maxCombustible;
    }

    public int getMaxGlp() {
        return maxGlp;
    }
}
