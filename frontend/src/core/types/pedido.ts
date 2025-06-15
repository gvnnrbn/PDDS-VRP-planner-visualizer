export interface Pedido {
  id: number;
  codigoCliente: string;
  fechaRegistro: string; // Will be ISO string from backend
  posicionX: number;
  posicionY: number;
  cantidadGLP: number;
  tiempoTolerancia: number;
}

export interface PedidoSimulado  {
  idPedido: number;
  estado: string;
  glp: number;
  posX: number;
  posY: number;
  fechaLimite: string,
  vehiculosAtendiendo: Array<{
      placa: string,
      eta: string
  }>
}