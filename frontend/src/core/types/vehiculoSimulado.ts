export interface VehiculoSimulado  {
  idVehiculo: number;
  tipo: string;
  combustible: number;
  currGLP: number;
  posicionX: number;
  posicionY: number;
  estado: string;
  accion: string;
  rutaActual?: { posX: number; posY: number }[];
}