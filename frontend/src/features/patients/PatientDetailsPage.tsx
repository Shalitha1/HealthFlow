import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ArrowLeft, CalendarDays, Cake, Mail, MapPin, Pencil, ReceiptText, UserRoundCheck, UserRoundX } from 'lucide-react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { patientApi } from '../../api/patientApi'
import { ErrorState } from '../../components/feedback/ErrorState'
import { LoadingState } from '../../components/feedback/LoadingState'
import { StatusBadge } from '../../components/feedback/StatusBadge'
import { PageHeader } from '../../components/layout/PageHeader'
import { formatDate, formatDateTime } from '../../utils/format'

export function PatientDetailsPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const patient = useQuery({ queryKey: ['patient', id], queryFn: () => patientApi.get(id!) })
  const statusMutation = useMutation({
    mutationFn: (active: boolean) => patientApi.updateStatus(id!, active),
    onSuccess: (updated) => {
      queryClient.setQueryData(['patient', id], updated)
      queryClient.invalidateQueries({ queryKey: ['patients'] })
      queryClient.invalidateQueries({ queryKey: ['patient-statistics'] })
    },
  })
  const deleteMutation = useMutation({
    mutationFn: () => patientApi.deactivate(id!),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['patients'] })
      queryClient.invalidateQueries({ queryKey: ['patient-statistics'] })
      navigate('/patients')
    },
  })

  if (patient.isLoading) return <LoadingState label="Opening patient record" />
  if (patient.isError || !patient.data) return <ErrorState message={patient.error?.message ?? 'Patient record not found.'} onRetry={() => patient.refetch()} />

  const record = patient.data

  return (
    <div className="space-y-7">
      <PageHeader
        eyebrow={`Patient HF-${String(record.id).padStart(5, '0')}`}
        title={record.name}
        description={`Registered ${formatDate(record.createdAt)} · Last updated ${formatDateTime(record.updatedAt)}`}
        actions={<><Link to="/patients" className="hidden items-center gap-2 rounded-xl border border-slate-200 bg-white px-3.5 py-2.5 text-sm font-semibold text-slate-700 hover:bg-slate-50 sm:inline-flex"><ArrowLeft className="h-4 w-4" /> Patients</Link><Link to={`/patients/${record.id}/edit`} className="inline-flex items-center gap-2 rounded-xl bg-sky-600 px-4 py-2.5 text-sm font-bold text-white hover:bg-sky-700"><Pencil className="h-4 w-4" /> Edit</Link></>}
      />

      <div className="grid gap-6 xl:grid-cols-[1.2fr_0.8fr]">
        <section className="rounded-2xl border border-slate-200/80 bg-white p-5 sm:p-7">
          <div className="flex items-center justify-between"><h2 className="font-bold text-slate-950">Patient information</h2><StatusBadge active={record.active} /></div>
          <div className="mt-6 grid gap-5 sm:grid-cols-2">
            {[{ label: 'Email address', value: record.email, icon: Mail }, { label: 'Date of birth', value: formatDate(record.dateOfBirth), icon: Cake }, { label: 'Address', value: record.address, icon: MapPin }, { label: 'Registration date', value: formatDateTime(record.createdAt), icon: CalendarDays }].map((item) => { const Icon = item.icon; return <div key={item.label} className="rounded-2xl bg-slate-50 p-4"><div className="flex items-center gap-2 text-xs font-bold uppercase tracking-wider text-slate-400"><Icon className="h-4 w-4" />{item.label}</div><p className="mt-2 break-words text-sm font-semibold leading-6 text-slate-800">{item.value}</p></div> })}
          </div>

          <div className="mt-7 border-t border-slate-100 pt-5">
            <h3 className="text-xs font-bold uppercase tracking-wider text-slate-400">Record status</h3>
            <div className="mt-3 flex flex-col gap-3 rounded-2xl border border-slate-200 p-4 sm:flex-row sm:items-center sm:justify-between"><div><p className="text-sm font-semibold text-slate-800">{record.active ? 'Active patient record' : 'Inactive historical record'}</p><p className="mt-1 text-xs leading-5 text-slate-500">Inactive records remain available in patient history and reporting.</p></div><button type="button" disabled={statusMutation.isPending || deleteMutation.isPending} onClick={() => { if (record.active && window.confirm('Mark this patient inactive?')) deleteMutation.mutate(); else statusMutation.mutate(true) }} className={`inline-flex shrink-0 items-center justify-center gap-2 rounded-xl px-4 py-2.5 text-sm font-bold ${record.active ? 'border border-rose-200 text-rose-700 hover:bg-rose-50' : 'bg-emerald-600 text-white hover:bg-emerald-700'}`}>{record.active ? <UserRoundX className="h-4 w-4" /> : <UserRoundCheck className="h-4 w-4" />}{record.active ? 'Deactivate' : 'Reactivate'}</button></div>
          </div>
        </section>

        <div className="space-y-6">
          {[{ title: 'Future appointments', description: 'Upcoming visits and care schedules will appear when the Appointment Service is connected.', icon: CalendarDays, color: 'bg-violet-50 text-violet-700' }, { title: 'Billing overview', description: 'Balances, claims, and billing activity will appear when billing read APIs are available.', icon: ReceiptText, color: 'bg-amber-50 text-amber-700' }].map((section) => { const Icon = section.icon; return <section key={section.title} className="rounded-2xl border border-slate-200/80 bg-white p-5"><div className={`grid h-10 w-10 place-items-center rounded-xl ${section.color}`}><Icon className="h-5 w-5" /></div><h2 className="mt-4 font-bold text-slate-950">{section.title}</h2><p className="mt-2 text-sm leading-6 text-slate-500">{section.description}</p><div className="mt-5 rounded-xl border border-dashed border-slate-200 py-5 text-center text-xs font-medium text-slate-400">No connected data yet</div></section> })}
        </div>
      </div>
    </div>
  )
}
