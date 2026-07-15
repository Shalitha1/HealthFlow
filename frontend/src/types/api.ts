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

export type AppointmentStatus =
  | 'SCHEDULED'
  | 'CONFIRMED'
  | 'COMPLETED'
  | 'CANCELLED'
  | 'NO_SHOW'

export interface Appointment {
  id: number
  patientId: number
  doctorId: number
  appointmentDateTime: string
  durationMinutes: number
  reason: string
  status: AppointmentStatus
  notes: string | null
  createdAt: string
  updatedAt: string
}

export interface AppointmentInput {
  patientId: number
  doctorId: number
  appointmentDateTime: string
  durationMinutes: number
  reason: string
  notes?: string
}

export interface AppointmentListParams {
  date?: string
  patientId?: number
  doctorId?: number
  status?: AppointmentStatus
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

export type InvoiceStatus = 'DRAFT' | 'ISSUED' | 'PARTIALLY_PAID' | 'PAID' | 'OVERDUE' | 'CANCELLED'

export interface BillingAccount {
  id: string
  patientId: string
  status: 'ACTIVE' | 'CLOSED'
  createdAt: string
}

export interface InvoiceItem {
  id: number
  description: string
  quantity: number
  unitPrice: number
  subtotal: number
}

export interface Payment {
  id: number
  amount: number
  paymentMethod: string
  paidAt: string
  reference: string | null
}

export interface Invoice {
  id: number
  billingAccountId: string
  patientId: string
  appointmentId: number | null
  invoiceNumber: string
  totalAmount: number
  paidAmount: number
  balance: number
  status: InvoiceStatus
  dueDate: string
  createdAt: string
  items: InvoiceItem[]
  payments: Payment[]
}

export interface PatientBilling {
  account: BillingAccount
  totalBilled: number
  totalPaid: number
  outstandingBalance: number
  invoices: Invoice[]
}

export interface CreateInvoiceInput {
  billingAccountId: string
  appointmentId?: number
  dueDate: string
  status: 'DRAFT' | 'ISSUED'
  items: Array<{ description: string; quantity: number; unitPrice: number }>
}

export interface PaymentInput {
  amount: number
  paymentMethod: string
  reference?: string
}

export interface BillingStatistics {
  totalInvoices: number
  outstandingInvoices: number
  overdueInvoices: number
  outstandingBalance: number
  paymentsReceived: number
}
