import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ArrowLeft, CalendarClock, Clock3, FileText, Pencil, Stethoscope, UserRound, X } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { appointmentApi } from '../../api/appointmentApi'
import { ApiRequestError } from '../../api/httpClient'
import { useAuth } from '../../auth/AuthProvider'
import { ErrorState } from '../../components/feedback/ErrorState'
import { LoadingState } from '../../components/feedback/LoadingState'
import { PageHeader } from '../../components/layout/PageHeader'
import type { AppointmentStatus } from '../../types/api'
import { formatDateTime } from '../../utils/format'
import { AppointmentStatusBadge } from './AppointmentStatusBadge'
import { dateTimeForApi, toDateTimeInput } from './appointmentUtils'

export function AppointmentDetailsPage() {
  const { id } = useParams()
  const { user } = useAuth()
  const queryClient = useQueryClient()
  const [rescheduleOpen, setRescheduleOpen] = useState(false)
  const [cancelOpen, setCancelOpen] = useState(false)
  const [newDateTime, setNewDateTime] = useState('')
  const [newDuration, setNewDuration] = useState(30)
  const [actionError, setActionError] = useState<string | null>(null)
  const query = useQuery({ queryKey: ['appointment', id], queryFn: () => appointmentApi.get(id!) })
  const appointment = query.data
  const canManage = user?.role === 'ADMIN' || user?.role === 'RECEPTIONIST'
  const terminal = appointment ? ['COMPLETED', 'CANCELLED', 'NO_SHOW'].includes(appointment.status) : false

  useEffect(() => {
    if (appointment) {
      setNewDateTime(toDateTimeInput(appointment.appointmentDateTime))
      setNewDuration(appointment.durationMinutes)
    }
  }, [appointment])

  const refresh = (saved?: typeof appointment) => {
    if (saved) queryClient.setQueryData(['appointment', id], saved)
    queryClient.invalidateQueries({ queryKey: ['appointments'] })
  }
  const statusMutation = useMutation({
    mutationFn: (status: AppointmentStatus) => appointmentApi.updateStatus(id!, status),
    onSuccess: (saved) => { refresh(saved); setActionError(null) },
    onError: (error) => setActionError(error instanceof ApiRequestError ? error.message : 'Status could not be updated.'),
  })
  const rescheduleMutation = useMutation({
    mutationFn: () => appointmentApi.update(id!, {
      patientId: appointment!.patientId, doctorId: appointment!.doctorId,
      appointmentDateTime: dateTimeForApi(newDateTime), durationMinutes: newDuration,
      reason: appointment!.reason, notes: appointment!.notes ?? '',
    }),
    onSuccess: (saved) => { refresh(saved); setRescheduleOpen(false); setActionError(null) },
    onError: (error) => setActionError(error instanceof ApiRequestError ? error.message : 'Appointment could not be rescheduled.'),
  })
  const cancelMutation = useMutation({
    mutationFn: () => appointmentApi.cancel(id!),
    onSuccess: async () => { setCancelOpen(false); setActionError(null); await query.refetch(); queryClient.invalidateQueries({ queryKey: ['appointments'] }) },
    onError: (error) => setActionError(error instanceof ApiRequestError ? error.message : 'Appointment could not be cancelled.'),
  })

  if (query.isLoading) return <LoadingState label="Loading appointment" />
  if (query.isError || !appointment) return <ErrorState message={query.error?.message ?? 'Appointment not found'} onRetry={() => query.refetch()} />

  return (
    <div className="mx-auto max-w-5xl space-y-7">
      <PageHeader eyebrow={`Appointment #${appointment.id}`} title={appointment.reason} description="Review timing, participants, status, and operational notes."
        actions={<Link to="/appointments" className="inline-flex items-center gap-2 rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-sm font-semibold text-slate-700 hover:bg-slate-50"><ArrowLeft className="h-4 w-4" /> All appointments</Link>} />

      <div className="grid gap-5 lg:grid-cols-[1.4fr_0.6fr]">
        <section className="rounded-2xl border border-slate-200 bg-white p-5 sm:p-7">
          <div className="flex items-center justify-between border-b border-slate-100 pb-5"><h2 className="font-bold text-slate-900">Appointment information</h2><AppointmentStatusBadge status={appointment.status} /></div>
          <dl className="mt-6 grid gap-6 sm:grid-cols-2">
            <div><dt className="flex items-center gap-2 text-xs font-bold uppercase tracking-wider text-slate-400"><CalendarClock className="h-4 w-4 text-sky-600" /> Date and time</dt><dd className="mt-2 text-sm font-semibold text-slate-800">{formatDateTime(appointment.appointmentDateTime)}</dd></div>
            <div><dt className="flex items-center gap-2 text-xs font-bold uppercase tracking-wider text-slate-400"><Clock3 className="h-4 w-4 text-sky-600" /> Duration</dt><dd className="mt-2 text-sm font-semibold text-slate-800">{appointment.durationMinutes} minutes</dd></div>
            <div><dt className="flex items-center gap-2 text-xs font-bold uppercase tracking-wider text-slate-400"><UserRound className="h-4 w-4 text-sky-600" /> Patient</dt><dd className="mt-2"><Link to={`/patients/${appointment.patientId}`} className="text-sm font-bold text-sky-700 hover:text-sky-800">Patient #{appointment.patientId}</Link></dd></div>
            <div><dt className="flex items-center gap-2 text-xs font-bold uppercase tracking-wider text-slate-400"><Stethoscope className="h-4 w-4 text-sky-600" /> Doctor</dt><dd className="mt-2 text-sm font-semibold text-slate-800">Doctor #{appointment.doctorId}</dd></div>
            <div className="sm:col-span-2"><dt className="flex items-center gap-2 text-xs font-bold uppercase tracking-wider text-slate-400"><FileText className="h-4 w-4 text-sky-600" /> Notes</dt><dd className="mt-2 whitespace-pre-wrap text-sm leading-7 text-slate-600">{appointment.notes || 'No notes were added.'}</dd></div>
          </dl>
        </section>

        <aside className="space-y-4">
          <section className="rounded-2xl border border-slate-200 bg-white p-5"><h2 className="font-bold text-slate-900">Actions</h2><div className="mt-4 space-y-2">
            {canManage && !terminal && <><button type="button" onClick={() => setRescheduleOpen(true)} className="flex w-full items-center gap-2 rounded-xl border border-slate-200 px-3.5 py-2.5 text-sm font-semibold text-slate-700 hover:bg-slate-50"><Pencil className="h-4 w-4" /> Quick reschedule</button><Link to={`/appointments/${appointment.id}/edit`} className="flex w-full items-center gap-2 rounded-xl border border-slate-200 px-3.5 py-2.5 text-sm font-semibold text-slate-700 hover:bg-slate-50"><FileText className="h-4 w-4" /> Edit all details</Link></>}
            {appointment.status === 'SCHEDULED' && <button type="button" onClick={() => statusMutation.mutate('CONFIRMED')} className="w-full rounded-xl bg-violet-50 px-3.5 py-2.5 text-left text-sm font-bold text-violet-700 hover:bg-violet-100">Confirm appointment</button>}
            {(appointment.status === 'SCHEDULED' || appointment.status === 'CONFIRMED') && <button type="button" onClick={() => statusMutation.mutate('COMPLETED')} className="w-full rounded-xl bg-emerald-50 px-3.5 py-2.5 text-left text-sm font-bold text-emerald-700 hover:bg-emerald-100">Mark completed</button>}
            {(appointment.status === 'SCHEDULED' || appointment.status === 'CONFIRMED') && <button type="button" onClick={() => statusMutation.mutate('NO_SHOW')} className="w-full rounded-xl bg-amber-50 px-3.5 py-2.5 text-left text-sm font-bold text-amber-700 hover:bg-amber-100">Mark no-show</button>}
            {canManage && !terminal && <button type="button" onClick={() => setCancelOpen(true)} className="w-full rounded-xl bg-rose-50 px-3.5 py-2.5 text-left text-sm font-bold text-rose-700 hover:bg-rose-100">Cancel appointment</button>}
          </div></section>
          <section className="rounded-2xl bg-slate-950 p-5 text-white"><p className="text-xs font-bold uppercase tracking-wider text-sky-300">Created</p><p className="mt-2 text-sm text-slate-300">{formatDateTime(appointment.createdAt)}</p><p className="mt-4 text-xs text-slate-400">Last updated {formatDateTime(appointment.updatedAt)}</p></section>
        </aside>
      </div>

      {actionError && <div className="rounded-xl border border-rose-100 bg-rose-50 px-4 py-3 text-sm text-rose-700" role="alert">{actionError}</div>}

      {rescheduleOpen && <div className="fixed inset-0 z-50 grid place-items-center bg-slate-950/35 p-4 backdrop-blur-sm" role="dialog" aria-modal="true" aria-labelledby="reschedule-title"><div className="w-full max-w-md rounded-2xl bg-white p-6 shadow-2xl"><div className="flex items-center justify-between"><h2 id="reschedule-title" className="text-lg font-bold">Reschedule appointment</h2><button onClick={() => setRescheduleOpen(false)} className="rounded-lg p-2 text-slate-400 hover:bg-slate-100" aria-label="Close"><X className="h-4 w-4" /></button></div><p className="mt-2 text-sm text-slate-500">The new time will be checked against both schedules.</p><div className="mt-5 space-y-4"><div><label className="text-sm font-semibold text-slate-700" htmlFor="newDateTime">New date and time</label><input id="newDateTime" type="datetime-local" value={newDateTime} onChange={(event) => setNewDateTime(event.target.value)} className="mt-2 w-full rounded-xl border border-slate-200 px-3.5 py-3 text-sm outline-none focus:border-sky-500" /></div><div><label className="text-sm font-semibold text-slate-700" htmlFor="newDuration">Duration</label><select id="newDuration" value={newDuration} onChange={(event) => setNewDuration(Number(event.target.value))} className="mt-2 w-full rounded-xl border border-slate-200 px-3.5 py-3 text-sm">{[15, 30, 45, 60, 90, 120].map((value) => <option key={value} value={value}>{value} minutes</option>)}</select></div></div><div className="mt-6 flex justify-end gap-2"><button onClick={() => setRescheduleOpen(false)} className="rounded-xl px-4 py-2.5 text-sm font-semibold text-slate-600">Keep current time</button><button onClick={() => rescheduleMutation.mutate()} disabled={!newDateTime || new Date(newDateTime) <= new Date() || rescheduleMutation.isPending} className="rounded-xl bg-sky-600 px-4 py-2.5 text-sm font-bold text-white disabled:opacity-50">{rescheduleMutation.isPending ? 'Checking…' : 'Save new time'}</button></div></div></div>}

      {cancelOpen && <div className="fixed inset-0 z-50 grid place-items-center bg-slate-950/35 p-4 backdrop-blur-sm" role="alertdialog" aria-modal="true" aria-labelledby="cancel-title"><div className="w-full max-w-md rounded-2xl bg-white p-6 shadow-2xl"><h2 id="cancel-title" className="text-lg font-bold text-slate-950">Cancel this appointment?</h2><p className="mt-2 text-sm leading-6 text-slate-500">The record will remain in history with a Cancelled status and an audit event will be published.</p><div className="mt-6 flex justify-end gap-2"><button onClick={() => setCancelOpen(false)} className="rounded-xl px-4 py-2.5 text-sm font-semibold text-slate-600">Keep appointment</button><button onClick={() => cancelMutation.mutate()} disabled={cancelMutation.isPending} className="rounded-xl bg-rose-600 px-4 py-2.5 text-sm font-bold text-white disabled:opacity-50">{cancelMutation.isPending ? 'Cancelling…' : 'Yes, cancel'}</button></div></div></div>}
    </div>
  )
}
