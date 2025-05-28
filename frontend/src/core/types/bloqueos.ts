export interface BloqueoSimulado  {
  idBloqueo: number;
  fechaInicio: string,
  fechaFin: string,
  segmentos: {posX: number; posY: number} [];
}