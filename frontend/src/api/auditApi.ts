import type { AuditEvent } from '../types/api'
import { apiRequest } from './httpClient'

export const auditApi = {
  list: () => apiRequest<AuditEvent[]>('/audit-logs'),
}
