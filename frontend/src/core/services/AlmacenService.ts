import { BaseService } from './BaseService'
import type { Almacen } from '../types/almacen'

export class AlmacenService extends BaseService {
  async getAllAlmacenes(): Promise<Almacen[]> {
    return this.get<Almacen[]>('/api/almacenes')
  }

  async getAlmacenById(id: number): Promise<Almacen> {
    return this.get<Almacen>(`/api/almacenes/${id}`)
  }

  async createAlmacen(almacen: Partial<Almacen>): Promise<Almacen> {
    return this.post<Partial<Almacen>, Almacen>('/api/almacenes', almacen)
  }

  async updateAlmacen(id: number, almacen: Partial<Almacen>): Promise<Almacen> {
    return this.put<Partial<Almacen>, Almacen>(`/api/almacenes/${id}`, almacen)
  }

  async deleteAlmacen(id: number): Promise<void> {
    return this.delete(`/api/almacenes/${id}`)
  }
}
