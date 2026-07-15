import type { InvoiceStatus } from '../../types/api'
import { titleCase } from '../../utils/format'

const colors: Record<InvoiceStatus, string> = {
  DRAFT: 'bg-slate-100 text-slate-600', ISSUED: 'bg-sky-50 text-sky-700',
  PARTIALLY_PAID: 'bg-violet-50 text-violet-700', PAID: 'bg-emerald-50 text-emerald-700',
  OVERDUE: 'bg-rose-50 text-rose-700', CANCELLED: 'bg-slate-100 text-slate-500',
}
export function BillingStatusBadge({ status }: { status: InvoiceStatus }) {
  return <span className={`inline-flex rounded-full px-2.5 py-1 text-xs font-bold ${colors[status]}`}>{titleCase(status)}</span>
}
