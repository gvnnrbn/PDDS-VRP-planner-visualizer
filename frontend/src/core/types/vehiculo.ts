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
  placa: string;
}

export interface IFlotaCard {
  id: number;
  placa: string;
  estado: "Entregando" | "Sin programación" | "En Mantenimiento" | "Reabasteciéndose" | "Averiado";
  eta: string;
  glp: number;
  combustible: number;
  maxCombustible: number;
  pedidoId: string;
}

export interface VehiculoSimulado  {
  idVehiculo: number;
  tipo: string;
  combustible: number;
  maxCombustible: number;
  maxGLP: number;
  currGLP: number;
  placa: string;
  posicionX: number;
  posicionY: number;
  rutaActual?: { posX: number; posY: number; estado: string; accion: string; traspasoGLP?: number;idPedido: number }[];
}