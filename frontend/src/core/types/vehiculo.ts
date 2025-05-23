export type TipoVehiculoId = 'TA' | 'TB' | 'TC' | 'TD';

export interface TipoVehiculo {
  name: TipoVehiculoId;
  peso: number;
  maxCombustible: number;
  maxGlp: number;
}

export interface Vehiculo {
  id: number;
  tipo: TipoVehiculo;
}
