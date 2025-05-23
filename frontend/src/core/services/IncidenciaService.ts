import { BaseService } from './BaseService'
import type { Incidencia } from '../types/incidencia'

export class IncidenciaService extends BaseService {
  async getAllIncidencias(): Promise<Incidencia[]> {
    return this.get<Incidencia[]>('/api/incidencias')
  }

  async getIncidenciaById(id: number): Promise<Incidencia> {
    return this.get<Incidencia>(`/api/incidencias/${id}`)
  }

  async createIncidencia(incidencia: Partial<Incidencia>): Promise<Incidencia> {
    return this.post<Partial<Incidencia>, Incidencia>('/api/incidencias', incidencia)
  }

  async updateIncidencia(id: number, incidencia: Partial<Incidencia>): Promise<Incidencia> {
    return this.put<Partial<Incidencia>, Incidencia>(`/api/incidencias/${id}`, incidencia)
  }

  async deleteIncidencia(id: number): Promise<void> {
    return this.delete(`/api/incidencias/${id}`)
  }
}
