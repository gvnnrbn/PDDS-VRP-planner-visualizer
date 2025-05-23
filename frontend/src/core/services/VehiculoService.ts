import { BaseService } from './BaseService'
import type { Vehiculo } from '../types/vehiculo'

export class VehiculoService extends BaseService {
  async getAllVehiculos(): Promise<Vehiculo[]> {
    return this.get<Vehiculo[]>('/api/vehiculos')
  }

  async getVehiculoById(id: number): Promise<Vehiculo> {
    return this.get<Vehiculo>(`/api/vehiculos/${id}`)
  }

  async createVehiculo(vehiculo: Partial<Vehiculo>): Promise<Vehiculo> {
    return this.post<Partial<Vehiculo>, Vehiculo>('/api/vehiculos', vehiculo)
  }

  async updateVehiculo(id: number, vehiculo: Partial<Vehiculo>): Promise<Vehiculo> {
    return this.put<Partial<Vehiculo>, Vehiculo>(`/api/vehiculos/${id}`, vehiculo)
  }

  async deleteVehiculo(id: number): Promise<void> {
    return this.delete(`/api/vehiculos/${id}`)
  }
}
