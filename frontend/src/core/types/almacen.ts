export interface AlmacenSimulado  {
  idAlmacen: number;
  posicion?: {posX:number; posY:number};
  currentGLP?: number;
  maxGLP?: number;
  isMain: boolean;
  wasVehicle: boolean
}


export interface Almacen {
  idAlmacen: number
  capacidadEfectivam3: number
  esPrincipal: boolean
  horarioAbastecimiento: string
  posicionX: number
  posicionY: number
}
