import type { ServiceHealth } from '../types/api'
import { apiRequest } from './httpClient'

export const systemApi = {
  gatewayHealth: () => apiRequest<ServiceHealth>('/actuator/health'),
}
