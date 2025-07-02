import { BaseService } from './BaseService'
import type { Pedido } from '../types/pedido'

export class PedidoService extends BaseService {
  async getAllPedidos(): Promise<Pedido[]> {
    return this.get<Pedido[]>('/api/pedidos')
  }

  async getPedidoById(id: number): Promise<Pedido> {
    return this.get<Pedido>(`/api/pedidos/${id}`)
  }

  async createPedido(pedido: Partial<Pedido>): Promise<Pedido> {
    return this.post<Partial<Pedido>, Pedido>('/api/pedidos', pedido)
  }

  async updatePedido(id: number, pedido: Partial<Pedido>): Promise<Pedido> {
    return this.put<Partial<Pedido>, Pedido>(`/api/pedidos/${id}`, pedido)
  }

  async deletePedido(id: number): Promise<void> {
    return this.delete(`/api/pedidos/${id}`)
  }


  async importarPedidos(file: File): Promise<string> {
    const formData = new FormData();
    formData.append('file', file);

    const response = await fetch('/api/pedidos/importar', {
      method: 'POST',
      body: formData,
    });

    if (!response.ok) {
      const errorText = await response.text();
      console.error('🔥 Error backend:', errorText);
      throw new Error(errorText); // <-- esto lanza correctamente
    }

    return response.text();
  }


}
