export type TipoVehiculo = 'TA' | 'TB' | 'TC' | 'TD';

export interface Vehiculo {
  id: number;
  tipo: TipoVehiculo;
  peso: number;
  maxCombustible: number;
  maxGlp: number;
  currCombustible: number;
  currGlp: number;
  posicionX: number;
  posicionY: number;
  disponible: boolean;
}

export interface IFlotaCard {
  id: number;
  combustible: number;
}
export interface VehiculoSimulado  {
  idVehiculo: number;
  estado: string;
  eta: string;
  tipo: string;
  glp: number;
  combustible: number;
  maxCombustible: number;
  pedidoId: string;
  currGLP: number;
  placa: string;
  posicionX: number;
  posicionY: number;
  accion: string;
  rutaActual?: { posX: number; posY: number }[];
}