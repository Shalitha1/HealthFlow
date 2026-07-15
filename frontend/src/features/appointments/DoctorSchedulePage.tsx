import { useQuery } from '@tanstack/react-query'
import { ArrowLeft, Clock3, Stethoscope } from 'lucide-react'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { appointmentApi } from '../../api/appointmentApi'
import { useAuth } from '../../auth/AuthProvider'
import { EmptyState } from '../../components/feedback/EmptyState'
import { ErrorState } from '../../components/feedback/ErrorState'
import { LoadingState } from '../../components/feedback/LoadingState'
import { PageHeader } from '../../components/layout/PageHeader'
import { AppointmentStatusBadge } from './AppointmentStatusBadge'
import { timeOnly, toDateInput } from './appointmentUtils'

export function DoctorSchedulePage() {
  const { user } = useAuth()
  const [date, setDate] = useState(toDateInput())
  const schedule = useQuery({
    queryKey: ['appointments', 'doctor-schedule', user?.userId, date],
    queryFn: () => appointmentApi.list({ doctorId: user!.userId, date }),
    enabled: Boolean(user),
  })

  return (
    <div className="mx-auto max-w-5xl space-y-7">
      <PageHeader eyebrow="Doctor workspace" title="Daily schedule" description="A focused view of the signed-in doctor’s appointments for one day."
        actions={<Link to="/appointments" className="inline-flex items-center gap-2 rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-sm font-semibold text-slate-700 hover:bg-slate-50"><ArrowLeft className="h-4 w-4" /> Calendar</Link>} />
      <section className="flex flex-col gap-4 rounded-2xl border border-slate-200 bg-white p-5 sm:flex-row sm:items-center sm:justify-between"><div className="flex items-center gap-3"><div className="grid h-11 w-11 place-items-center rounded-xl bg-sky-50 text-sky-700"><Stethoscope className="h-5 w-5" /></div><div><p className="text-sm font-bold text-slate-900">Doctor #{user?.userId}</p><p className="mt-0.5 text-xs text-slate-500">{schedule.data?.length ?? 0} appointments</p></div></div><input type="date" value={date} onChange={(event) => setDate(event.target.value)} className="rounded-xl border border-slate-200 px-3.5 py-2.5 text-sm outline-none focus:border-sky-500" aria-label="Schedule date" /></section>

      {schedule.isLoading ? <LoadingState label="Loading daily schedule" /> : schedule.isError ? <ErrorState message={schedule.error.message} onRetry={() => schedule.refetch()} /> : schedule.data?.length === 0 ? <EmptyState title="No appointments today" message="There are no appointments assigned to you for this date." /> : (
        <section className="rounded-2xl border border-slate-200 bg-white p-4 sm:p-5"><div className="relative space-y-3 before:absolute before:bottom-4 before:left-[3.35rem] before:top-4 before:w-px before:bg-slate-200">{schedule.data?.map((appointment) => <Link key={appointment.id} to={`/appointments/${appointment.id}`} className="relative grid grid-cols-[4.5rem_1fr] gap-4 rounded-xl p-3 hover:bg-sky-50/50"><div className="pt-1 text-sm font-bold text-sky-700">{timeOnly(appointment.appointmentDateTime)}</div><div className="rounded-xl border border-slate-200 bg-white p-4 shadow-sm"><div className="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between"><div><p className="font-bold text-slate-900">{appointment.reason}</p><p className="mt-1 flex items-center gap-1.5 text-xs text-slate-500"><Clock3 className="h-3.5 w-3.5" /> {appointment.durationMinutes} minutes · Patient #{appointment.patientId}</p></div><AppointmentStatusBadge status={appointment.status} /></div></div></Link>)}</div></section>
      )}
    </div>
  )
}
