import type { BillingAccount, BillingStatistics, CreateInvoiceInput, Invoice, InvoiceStatus, PatientBilling, PaymentInput } from '../types/api'
import { apiRequest } from './httpClient'

export const billingApi = {
  createAccount: (patient: { id: number; name: string; email: string }) =>
    apiRequest<{ accountId: string; status: string }>('/api/billing/accounts', {
      method: 'POST',
      body: JSON.stringify({ patientId: String(patient.id), name: patient.name, email: patient.email }),
    }),
  patient: (patientId: number | string) => apiRequest<PatientBilling>(`/api/billing/accounts/patient/${patientId}`),
  invoices: (params: { status?: InvoiceStatus; patientId?: string } = {}) => {
    const query = new URLSearchParams()
    if (params.status) query.set('status', params.status)
    if (params.patientId) query.set('patientId', params.patientId)
    return apiRequest<Invoice[]>(`/api/billing/invoices${query.size ? `?${query}` : ''}`)
  },
  invoice: (id: number | string) => apiRequest<Invoice>(`/api/billing/invoices/${id}`),
  createInvoice: (input: CreateInvoiceInput) => apiRequest<Invoice>('/api/billing/invoices', { method: 'POST', body: JSON.stringify(input) }),
  recordPayment: (id: number | string, input: PaymentInput) => apiRequest<Invoice>(`/api/billing/invoices/${id}/payments`, { method: 'POST', body: JSON.stringify(input) }),
  statistics: () => apiRequest<BillingStatistics>('/api/billing/statistics'),
}

export type { BillingAccount }
