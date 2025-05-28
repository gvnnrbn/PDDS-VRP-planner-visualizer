export interface AlmacenSimulado  {
  idAlmacen: number;
  posicion?: {posX:number; posY:number};
  currentGLP?: number;
  maxGLP?: number;
  isMain: boolean;
  wasVehicle: boolean
}