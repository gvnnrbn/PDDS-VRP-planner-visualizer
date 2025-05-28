import type { TipoVehiculo } from "./vehiculo";

export interface IMantenimientoCard {
    id: number;
    vehiculo: {
        placa: string;
        tipo: TipoVehiculo
    }
    estado: "En Curso" | "Programado" | "Terminado";
    fechaInicio: string; // ISO string
    fechaFin: string; // ISO string
}