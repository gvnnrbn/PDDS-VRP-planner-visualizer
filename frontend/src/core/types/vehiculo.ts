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

export interface VehiculoSimulado  {
  idVehiculo: number;
  estado: string;
  tipo: string;
  combustible: number;
  maxCombustible: number;
  maxGLP: number;
  currGLP: number;
  placa: string;
  posicionX: number;
  posicionY: number;
  rutaActual?: {accion: string; cantidad:number; idEntidad: number; puntos: {posX: number; posY:number}[]}[];
}