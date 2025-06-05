export interface Incidencia {
    id: number;
    fecha: string;
    turno: "T1" | "T2" | "T3";
    vehiculo: {
        id: number;
    };
    ocurrido: boolean;
}

export interface IncidenciaSimulada {
    idIncidencia: number;
    fechaInicio: string;
    fechaFin: string;
    turno: string;
    tipo: string;
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