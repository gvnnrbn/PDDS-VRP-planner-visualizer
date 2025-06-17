export interface MantenimientoSimulado {
    idMantenimiento: number;
    vehiculo: {
        placa: string;
        tipo: string
    }
    estado: string;
    fechaInicio: string;
    fechaFin: string; 
}