import type {
  Patient,
  PatientInput,
  PatientListParams,
  PatientPage,
  PatientStatistics,
} from '../types/api'
import { apiRequest } from './httpClient'

function queryString(params: PatientListParams) {
  const query = new URLSearchParams()
  query.set('page', String(params.page ?? 0))
  query.set('size', String(params.size ?? 20))
  if (params.search?.trim()) query.set('search', params.search.trim())
  if (params.active !== undefined) query.set('active', String(params.active))
  return query.toString()
}

export const patientApi = {
  list: (params: PatientListParams) =>
    apiRequest<PatientPage>(`/api/patients?${queryString(params)}`),

  get: (id: number | string) => apiRequest<Patient>(`/api/patients/${id}`),

  create: (request: PatientInput) =>
    apiRequest<Patient>('/api/patients', {
      method: 'POST',
      body: JSON.stringify(request),
    }),

  update: (id: number | string, request: PatientInput) =>
    apiRequest<Patient>(`/api/patients/${id}`, {
      method: 'PUT',
      body: JSON.stringify(request),
    }),

  updateStatus: (id: number | string, active: boolean) =>
    apiRequest<Patient>(`/api/patients/${id}/status`, {
      method: 'PATCH',
      body: JSON.stringify({ active }),
    }),

  deactivate: (id: number | string) =>
    apiRequest<void>(`/api/patients/${id}`, { method: 'DELETE' }),

  statistics: () =>
    apiRequest<PatientStatistics>('/api/patients/statistics'),
}
