export interface Pedido {
  id: number;
  codigoCliente: string;
  fechaRegistro: string; // Will be ISO string from backend
  posicionX: number;
  posicionY: number;
  cantidadGLP: number;
  tiempoTolerancia: number;
}
