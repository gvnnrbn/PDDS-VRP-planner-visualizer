
export interface IFlotaCard {
    id: number;
    placa: string;
    estado: "Entregando" | "Sin programación" | "En Mantenimiento" | "Reabasteciéndose" | "Averiado";
    eta: string;
    glp: number;
    
}