import { useQuery } from '@tanstack/react-query'
import { ArrowLeft, FileText, Plus } from 'lucide-react'
import { Link, useParams } from 'react-router-dom'
import { billingApi } from '../../api/billingApi'
import { patientApi } from '../../api/patientApi'
import { ErrorState } from '../../components/feedback/ErrorState'
import { LoadingState } from '../../components/feedback/LoadingState'
import { PageHeader } from '../../components/layout/PageHeader'
import { formatCurrency, formatDate } from '../../utils/format'
import { BillingStatusBadge } from './BillingStatusBadge'

export function PatientBillingPage() {
  const { patientId } = useParams(); const patient = useQuery({ queryKey: ['patient', patientId], queryFn: () => patientApi.get(patientId!) }); const billing = useQuery({ queryKey: ['patient-billing', patientId], queryFn: () => billingApi.patient(patientId!) })
  if (patient.isLoading || billing.isLoading) return <LoadingState label="Loading patient billing" />
  if (patient.isError || billing.isError || !patient.data || !billing.data) return <ErrorState message={patient.error?.message ?? billing.error?.message ?? 'Billing account not found'} onRetry={() => { patient.refetch(); billing.refetch() }} />
  return <div className="space-y-7"><PageHeader eyebrow={`Patient HF-${String(patientId).padStart(5, '0')}`} title={`${patient.data.name} · Billing`} description={`Account ${billing.data.account.id.slice(0, 8)} · Created ${formatDate(billing.data.account.createdAt)}`} actions={<><Link to={`/patients/${patientId}`} className="inline-flex items-center gap-2 rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-sm font-semibold"><ArrowLeft className="h-4 w-4" /> Patient</Link><Link to={`/billing/invoices/new?patientId=${patientId}`} className="inline-flex items-center gap-2 rounded-xl bg-sky-600 px-4 py-2.5 text-sm font-bold text-white"><Plus className="h-4 w-4" /> Create invoice</Link></>} />
    <div className="grid gap-4 sm:grid-cols-3">{[['Total billed', billing.data.totalBilled], ['Payments received', billing.data.totalPaid], ['Outstanding balance', billing.data.outstandingBalance]].map(([label, amount]) => <div key={String(label)} className="rounded-2xl border border-slate-200 bg-white p-5"><p className="text-sm text-slate-500">{label}</p><p className="mt-2 text-2xl font-bold">{formatCurrency(Number(amount))}</p></div>)}</div>
    <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white"><div className="border-b border-slate-100 px-5 py-4"><h2 className="font-bold">Invoice history</h2></div>{billing.data.invoices.length ? <div className="divide-y divide-slate-100">{billing.data.invoices.map((invoice) => <Link key={invoice.id} to={`/billing/invoices/${invoice.id}`} className="grid gap-2 px-5 py-4 hover:bg-slate-50 sm:grid-cols-[1fr_1fr_auto_auto] sm:items-center"><div className="flex items-center gap-2"><FileText className="h-4 w-4 text-sky-600" /><span className="text-sm font-bold">{invoice.invoiceNumber}</span></div><span className="text-sm text-slate-500">Due {formatDate(invoice.dueDate)}</span><span className="text-sm font-bold">{formatCurrency(invoice.balance)}</span><BillingStatusBadge status={invoice.status} /></Link>)}</div> : <p className="p-8 text-center text-sm text-slate-500">No invoices for this patient.</p>}</section>
  </div>
}
