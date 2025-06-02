export interface Incidencia {
    id: number;
    fecha: string;
    turno: "T1" | "T2" | "T3";
    vehiculo: {
        id: number;
    };
    ocurrido: boolean;
}

export interface IIncidenciaCard {
    id: number;
    fechaInicio: string;
    fechaFin: string;
    turno: "T1" | "T2" | "T3";
    tipo: "TI1" | "TI2" | "TI3";
    placa: string;
    estado: string;
}

export interface IncidenciaSimulado{
    idIncidencia: number;
    fechaInicio: string;
    fechaFin: string;
    turno: string;
    tipo: string;
    placa: string;
    estado: string;
}