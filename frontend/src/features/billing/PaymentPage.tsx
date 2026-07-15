import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ArrowLeft, Banknote } from 'lucide-react'
import { useForm } from 'react-hook-form'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { z } from 'zod'
import { billingApi } from '../../api/billingApi'
import { ApiRequestError } from '../../api/httpClient'
import { ErrorState } from '../../components/feedback/ErrorState'
import { LoadingState } from '../../components/feedback/LoadingState'
import { PageHeader } from '../../components/layout/PageHeader'
import { formatCurrency } from '../../utils/format'
const schema = z.object({ amount: z.number().positive('Enter a payment amount'), paymentMethod: z.string().min(1, 'Select a payment method'), reference: z.string().max(120).optional() })
type Values = z.infer<typeof schema>
export function PaymentPage() {
  const { id } = useParams(); const navigate = useNavigate(); const client = useQueryClient(); const invoice = useQuery({ queryKey: ['billing-invoice', id], queryFn: () => billingApi.invoice(id!) })
  const { register, handleSubmit, setError, formState: { errors } } = useForm<Values>({ resolver: zodResolver(schema), values: invoice.data ? { amount: invoice.data.balance, paymentMethod: 'CARD', reference: '' } : undefined })
  const mutation = useMutation({ mutationFn: (values: Values) => billingApi.recordPayment(id!, values), onSuccess: (saved) => { client.setQueryData(['billing-invoice', id], saved); client.invalidateQueries({ queryKey: ['billing-invoices'] }); client.invalidateQueries({ queryKey: ['billing-statistics'] }); navigate(`/billing/invoices/${id}`) }, onError: (error) => setError('root', { message: error instanceof ApiRequestError ? error.message : 'Payment could not be recorded.' }) })
  if (invoice.isLoading) return <LoadingState label="Loading invoice balance" />
  if (invoice.isError || !invoice.data) return <ErrorState message={invoice.error?.message ?? 'Invoice not found'} onRetry={() => invoice.refetch()} />
  const input = 'mt-2 w-full rounded-xl border border-slate-200 bg-white px-3.5 py-3 text-sm outline-none focus:border-sky-500 focus:ring-4 focus:ring-sky-100'
  return <div className="mx-auto max-w-2xl space-y-7"><PageHeader eyebrow={invoice.data.invoiceNumber} title="Record payment" description={`Outstanding balance: ${formatCurrency(invoice.data.balance)}`} actions={<Link to={`/billing/invoices/${id}`} className="inline-flex items-center gap-2 rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-sm font-semibold"><ArrowLeft className="h-4 w-4" /> Invoice</Link>} /><form onSubmit={handleSubmit((values) => mutation.mutate(values))} className="rounded-2xl border border-slate-200 bg-white p-6 sm:p-8"><div className="rounded-2xl bg-emerald-50 p-4 text-sm text-emerald-800">Payments are final in this milestone. The server locks the invoice and rejects amounts above its current balance.</div><div className="mt-6 space-y-5"><div><label className="text-sm font-semibold" htmlFor="amount">Amount</label><input id="amount" type="number" min="0.01" max={invoice.data.balance} step="0.01" className={input} {...register('amount', { valueAsNumber: true })} />{errors.amount && <p className="mt-1 text-xs text-rose-600">{errors.amount.message}</p>}</div><div><label className="text-sm font-semibold" htmlFor="paymentMethod">Payment method</label><select id="paymentMethod" className={input} {...register('paymentMethod')}><option value="CARD">Card</option><option value="CASH">Cash</option><option value="BANK_TRANSFER">Bank transfer</option><option value="INSURANCE">Insurance</option><option value="OTHER">Other</option></select></div><div><label className="text-sm font-semibold" htmlFor="reference">Reference <span className="font-normal text-slate-400">(optional)</span></label><input id="reference" className={input} placeholder="Receipt or transaction reference" {...register('reference')} /></div></div>{errors.root && <div className="mt-5 rounded-xl bg-rose-50 p-4 text-sm text-rose-700">{errors.root.message}</div>}<button disabled={mutation.isPending} className="mt-7 inline-flex w-full items-center justify-center gap-2 rounded-xl bg-emerald-600 px-5 py-3 text-sm font-bold text-white disabled:opacity-60"><Banknote className="h-4 w-4" /> {mutation.isPending ? 'Recording…' : 'Record payment'}</button></form></div>
}
