import { useQuery } from '@tanstack/react-query'
import { AlertCircle, Banknote, FileText, Plus, ReceiptText } from 'lucide-react'
import { Link } from 'react-router-dom'
import { billingApi } from '../../api/billingApi'
import { ErrorState } from '../../components/feedback/ErrorState'
import { LoadingState } from '../../components/feedback/LoadingState'
import { PageHeader } from '../../components/layout/PageHeader'
import { formatCurrency } from '../../utils/format'

export function BillingDashboardPage() {
  const stats = useQuery({ queryKey: ['billing-statistics'], queryFn: billingApi.statistics })
  const invoices = useQuery({ queryKey: ['billing-invoices', 'outstanding'], queryFn: () => billingApi.invoices() })
  const outstanding = invoices.data?.filter((item) => ['ISSUED', 'PARTIALLY_PAID', 'OVERDUE'].includes(item.status)).slice(0, 6) ?? []
  return <div className="space-y-7">
    <PageHeader eyebrow="Revenue operations" title="Outstanding balances" description="Monitor open invoices, overdue balances, and collected payments from one workspace." actions={<><Link to="/billing/invoices" className="rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-sm font-bold text-slate-700 hover:bg-slate-50">All invoices</Link><Link to="/billing/invoices/new" className="inline-flex items-center gap-2 rounded-xl bg-sky-600 px-4 py-2.5 text-sm font-bold text-white hover:bg-sky-700"><Plus className="h-4 w-4" /> Create invoice</Link></>} />
    {stats.isLoading ? <LoadingState label="Loading billing totals" /> : stats.isError ? <ErrorState message={stats.error.message} onRetry={() => stats.refetch()} /> : <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">{[
      { label: 'Outstanding balance', value: formatCurrency(stats.data?.outstandingBalance ?? 0), icon: ReceiptText, tint: 'bg-amber-50 text-amber-700' },
      { label: 'Open invoices', value: String(stats.data?.outstandingInvoices ?? 0), icon: FileText, tint: 'bg-sky-50 text-sky-700' },
      { label: 'Overdue invoices', value: String(stats.data?.overdueInvoices ?? 0), icon: AlertCircle, tint: 'bg-rose-50 text-rose-700' },
      { label: 'Payments received', value: formatCurrency(stats.data?.paymentsReceived ?? 0), icon: Banknote, tint: 'bg-emerald-50 text-emerald-700' },
    ].map(({ label, value, icon: Icon, tint }) => <div key={label} className="rounded-2xl border border-slate-200 bg-white p-5"><div className={`grid h-10 w-10 place-items-center rounded-xl ${tint}`}><Icon className="h-5 w-5" /></div><p className="mt-5 text-2xl font-bold text-slate-950">{value}</p><p className="mt-1 text-sm text-slate-500">{label}</p></div>)}</div>}
    <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white"><div className="flex items-center justify-between border-b border-slate-100 px-5 py-4"><div><h2 className="font-bold">Balances needing attention</h2><p className="mt-1 text-xs text-slate-500">Issued, partially paid, and overdue invoices</p></div><Link to="/billing/invoices" className="text-xs font-bold text-sky-700">View all</Link></div>{invoices.isError ? <ErrorState message={invoices.error.message} onRetry={() => invoices.refetch()} /> : outstanding.length ? <div className="divide-y divide-slate-100">{outstanding.map((invoice) => <Link key={invoice.id} to={`/billing/invoices/${invoice.id}`} className="grid gap-2 px-5 py-4 hover:bg-slate-50 sm:grid-cols-[1fr_1fr_auto] sm:items-center"><div><p className="text-sm font-bold">{invoice.invoiceNumber}</p><p className="text-xs text-slate-500">Patient HF-{String(invoice.patientId).padStart(5, '0')}</p></div><p className="text-sm text-slate-500">Due {invoice.dueDate}</p><p className="font-bold text-slate-900">{formatCurrency(invoice.balance)}</p></Link>)}</div> : <p className="p-8 text-center text-sm text-slate-500">No outstanding balances.</p>}</section>
  </div>
}
