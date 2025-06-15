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
  eta: string;
  tipo: string;
  combustible: number;
  maxCombustible: number;
  currGLP: number;
  maxGLP: number;
  placa: string;
  posicionX: number;
  posicionY: number;
  idPedido: number;
  rutaActual?: Array<{ 
    posX: number; 
    posY: number
  }>;
}