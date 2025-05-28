export interface Pedido {
  id: number;
  codigoCliente: string;
  fechaRegistro: string; // Will be ISO string from backend
  posicionX: number;
  posicionY: number;
  cantidadGLP: number;
  tiempoTolerancia: number;
  // falta vehículos que lo están atendiendo
}
export interface IOrderCard{
  id: string,
  state: string,
  glp: number,
  deadline: string,
  vehicles: Array<{
      plaque: string,
      eta: string
  }>
}

export interface PedidoSimulado  {
  idPedido: number;
  posX: number;
  posY: number;
}