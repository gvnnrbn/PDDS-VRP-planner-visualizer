import type { TipoVehiculo } from "./vehiculo";

export interface MantenimientoSimulado {
    idMantenimiento: number;
    vehiculo: {
        placa: string;
        tipo: TipoVehiculo
    }
    estado: "En Curso" | "Programado" | "Terminado";
    fechaInicio: string;
    fechaFin: string; 
}