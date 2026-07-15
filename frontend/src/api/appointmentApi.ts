import type {
  Appointment,
  AppointmentInput,
  AppointmentListParams,
  AppointmentStatus,
} from '../types/api'
import { apiRequest } from './httpClient'

function queryString(params: AppointmentListParams) {
  const query = new URLSearchParams()
  if (params.date) query.set('date', params.date)
  if (params.patientId) query.set('patientId', String(params.patientId))
  if (params.doctorId) query.set('doctorId', String(params.doctorId))
  if (params.status) query.set('status', params.status)
  const value = query.toString()
  return value ? `?${value}` : ''
}

export const appointmentApi = {
  list: (params: AppointmentListParams = {}) =>
    apiRequest<Appointment[]>(`/api/appointments${queryString(params)}`),
  get: (id: number | string) => apiRequest<Appointment>(`/api/appointments/${id}`),
  create: (request: AppointmentInput) => apiRequest<Appointment>('/api/appointments', {
    method: 'POST', body: JSON.stringify(request),
  }),
  update: (id: number | string, request: AppointmentInput) =>
    apiRequest<Appointment>(`/api/appointments/${id}`, {
      method: 'PUT', body: JSON.stringify(request),
    }),
  updateStatus: (id: number | string, status: AppointmentStatus) =>
    apiRequest<Appointment>(`/api/appointments/${id}/status`, {
      method: 'PATCH', body: JSON.stringify({ status }),
    }),
  cancel: (id: number | string) =>
    apiRequest<void>(`/api/appointments/${id}`, { method: 'DELETE' }),
}
