package pucp.pdds.backend.model;

public enum TipoVehiculo {
    TA(1000, 50, 20),
    TB(1500, 60, 30),
    TC(2000, 70, 40),
    TD(2500, 80, 50);

    private final int peso;
    private final int maxCombustible;
    private final int maxGlp;

    TipoVehiculo(int peso, int maxCombustible, int maxGlp) {
        this.peso = peso;
        this.maxCombustible = maxCombustible;
        this.maxGlp = maxGlp;
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
