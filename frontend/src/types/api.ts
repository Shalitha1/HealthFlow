export type UserRole = 'ADMIN' | 'RECEPTIONIST' | 'DOCTOR'

export interface CurrentUser {
  userId: number
  email: string
  role: UserRole
}

export interface LoginRequest {
  email: string
  password: string
}

export interface RegisterRequest {
  email: string
  password: string
}

export interface LoginResponse {
  token: string
}

export interface ApiErrorBody {
  timestamp: string
  status: number
  errorCode: string
  message: string
  path: string
  fieldErrors: Record<string, string>
}

export interface Patient {
  id: number
  name: string
  email: string
  address: string
  dateOfBirth: string
  createdAt: string
  updatedAt: string
  active: boolean
}

export interface PatientInput {
  name: string
  email: string
  address: string
  dateOfBirth: string
}

export interface PatientPage {
  patients: Patient[]
  currentPage: number
  pageSize: number
  totalElements: number
  totalPages: number
}

export interface PatientListParams {
  page?: number
  size?: number
  search?: string
  active?: boolean
}

export interface PatientStatistics {
  totalPatients: number
  activePatients: number
  inactivePatients: number
  registrationsThisMonth: number
}

export interface AuditEvent {
  id: number
  eventType: string
  payload: string
  actorId: string | null
  timestamp: string
}

export interface ServiceHealth {
  status: string
}
