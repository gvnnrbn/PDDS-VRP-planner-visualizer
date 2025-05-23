export interface Incidencia {
    id: number;
    fecha: string;
    turno: "T1" | "T2" | "T3";
    vehiculo: {
        id: number;
    };
    ocurrido: boolean;
}