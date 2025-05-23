import { BaseService } from './BaseService'
import type { TipoVehiculo, TipoVehiculoId } from '../types/vehiculo'

export class TipoVehiculoService extends BaseService {
  async getAllTiposVehiculos(): Promise<TipoVehiculo[]> {
    return this.get<TipoVehiculo[]>('/api/tipos-vehiculos')
  }

  async getTipoVehiculoById(id: TipoVehiculoId): Promise<TipoVehiculo> {
    return this.get<TipoVehiculo>(`/api/tipos-vehiculos/${id}`)
  }
}
