import axios from 'axios'

export class BaseService {
  private readonly baseUrl: string

  constructor() {
    this.baseUrl = import.meta.env.VITE_API_URL || 'http://localhost:8080'
    
    // Configure axios defaults
    axios.defaults.baseURL = this.baseUrl
    axios.defaults.headers.post['Content-Type'] = 'application/json'
  }

  protected async get<T>(endpoint: string): Promise<T> {
    const response = await axios.get<T>(endpoint)
    return response.data
  }

  protected async post<T, R>(endpoint: string, data: T): Promise<R> {
    const response = await axios.post<R>(endpoint, data)
    return response.data
  }

  protected async put<T, R>(endpoint: string, data: T): Promise<R> {
    const response = await axios.put<R>(endpoint, data)
    return response.data
  }

  protected async delete(endpoint: string): Promise<void> {
    await axios.delete(endpoint)
  }
}
