import { useQuery } from '@tanstack/react-query'
import { Plus, Search } from 'lucide-react'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { billingApi } from '../../api/billingApi'
import { EmptyState } from '../../components/feedback/EmptyState'
import { ErrorState } from '../../components/feedback/ErrorState'
import { LoadingState } from '../../components/feedback/LoadingState'
import { PageHeader } from '../../components/layout/PageHeader'
import type { InvoiceStatus } from '../../types/api'
import { formatCurrency, formatDate } from '../../utils/format'
import { BillingStatusBadge } from './BillingStatusBadge'

const statuses: InvoiceStatus[] = ['DRAFT', 'ISSUED', 'PARTIALLY_PAID', 'PAID', 'OVERDUE', 'CANCELLED']
export function InvoiceListPage() {
  const [status, setStatus] = useState<InvoiceStatus | ''>('')
  const [patientId, setPatientId] = useState('')
  const query = useQuery({ queryKey: ['billing-invoices', status, patientId], queryFn: () => billingApi.invoices({ status: status || undefined, patientId: patientId.trim() || undefined }) })
  return <div className="space-y-7"><PageHeader eyebrow="Billing records" title="Invoices" description="Review every invoice, payment state, due date, and outstanding balance." actions={<Link to="/billing/invoices/new" className="inline-flex items-center gap-2 rounded-xl bg-sky-600 px-4 py-2.5 text-sm font-bold text-white hover:bg-sky-700"><Plus className="h-4 w-4" /> Create invoice</Link>} />
    <section className="grid gap-3 rounded-2xl border border-slate-200 bg-white p-4 sm:grid-cols-[1fr_240px]"><label className="relative"><Search className="absolute left-3.5 top-3 h-4 w-4 text-slate-400" /><input value={patientId} onChange={(e) => setPatientId(e.target.value)} placeholder="Filter by patient ID" className="w-full rounded-xl border border-slate-200 py-2.5 pl-10 pr-3 text-sm outline-none focus:border-sky-500" /></label><select value={status} onChange={(e) => setStatus(e.target.value as InvoiceStatus | '')} className="rounded-xl border border-slate-200 px-3 py-2.5 text-sm"><option value="">All statuses</option>{statuses.map((item) => <option key={item} value={item}>{item.replace('_', ' ')}</option>)}</select></section>
    {query.isLoading ? <LoadingState label="Loading invoices" /> : query.isError ? <ErrorState message={query.error.message} onRetry={() => query.refetch()} /> : !query.data?.length ? <EmptyState title="No matching invoices" message="Change the filters or create the first invoice." /> : <section className="overflow-x-auto rounded-2xl border border-slate-200 bg-white"><table className="w-full min-w-[760px] text-left"><thead className="bg-slate-50 text-xs uppercase tracking-wider text-slate-500"><tr><th className="px-5 py-3">Invoice</th><th className="px-5 py-3">Patient</th><th className="px-5 py-3">Due date</th><th className="px-5 py-3">Total</th><th className="px-5 py-3">Balance</th><th className="px-5 py-3">Status</th></tr></thead><tbody className="divide-y divide-slate-100">{query.data.map((invoice) => <tr key={invoice.id} className="hover:bg-sky-50/30"><td className="px-5 py-4"><Link to={`/billing/invoices/${invoice.id}`} className="font-bold text-sky-700">{invoice.invoiceNumber}</Link><p className="mt-1 text-xs text-slate-400">Created {formatDate(invoice.createdAt)}</p></td><td className="px-5 py-4"><Link to={`/billing/accounts/patient/${invoice.patientId}`} className="text-sm font-semibold text-slate-700">HF-{String(invoice.patientId).padStart(5, '0')}</Link></td><td className="px-5 py-4 text-sm text-slate-600">{formatDate(invoice.dueDate)}</td><td className="px-5 py-4 text-sm font-semibold">{formatCurrency(invoice.totalAmount)}</td><td className="px-5 py-4 text-sm font-bold">{formatCurrency(invoice.balance)}</td><td className="px-5 py-4"><BillingStatusBadge status={invoice.status} /></td></tr>)}</tbody></table></section>}
  </div>
}
