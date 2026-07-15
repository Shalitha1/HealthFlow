import { useQuery } from '@tanstack/react-query'
import { CalendarDays, ChevronLeft, ChevronRight, Clock3, List, Plus, Stethoscope } from 'lucide-react'
import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { appointmentApi } from '../../api/appointmentApi'
import { useAuth } from '../../auth/AuthProvider'
import { EmptyState } from '../../components/feedback/EmptyState'
import { ErrorState } from '../../components/feedback/ErrorState'
import { LoadingState } from '../../components/feedback/LoadingState'
import { PageHeader } from '../../components/layout/PageHeader'
import type { Appointment, AppointmentStatus } from '../../types/api'
import { formatDateTime } from '../../utils/format'
import { AppointmentStatusBadge } from './AppointmentStatusBadge'
import { timeOnly, toDateInput } from './appointmentUtils'

type ViewMode = 'calendar' | 'list'
const statuses: AppointmentStatus[] = ['SCHEDULED', 'CONFIRMED', 'COMPLETED', 'CANCELLED', 'NO_SHOW']

function CalendarView({ appointments, month, changeMonth }: { appointments: Appointment[]; month: Date; changeMonth: (amount: number) => void }) {
  const days = useMemo(() => {
    const first = new Date(month.getFullYear(), month.getMonth(), 1)
    const start = new Date(first)
    start.setDate(1 - first.getDay())
    return Array.from({ length: 42 }, (_, index) => {
      const date = new Date(start)
      date.setDate(start.getDate() + index)
      return date
    })
  }, [month])

  return (
    <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white">
      <div className="flex items-center justify-between border-b border-slate-100 px-4 py-4 sm:px-5">
        <button type="button" onClick={() => changeMonth(-1)} className="rounded-xl border border-slate-200 p-2 text-slate-600 hover:bg-slate-50" aria-label="Previous month"><ChevronLeft className="h-4 w-4" /></button>
        <h2 className="font-bold text-slate-900">{new Intl.DateTimeFormat('en', { month: 'long', year: 'numeric' }).format(month)}</h2>
        <button type="button" onClick={() => changeMonth(1)} className="rounded-xl border border-slate-200 p-2 text-slate-600 hover:bg-slate-50" aria-label="Next month"><ChevronRight className="h-4 w-4" /></button>
      </div>
      <div className="grid grid-cols-7 border-b border-slate-100 bg-slate-50 text-center text-[10px] font-bold uppercase tracking-wider text-slate-500">
        {['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'].map((day) => <div key={day} className="py-2.5">{day}</div>)}
      </div>
      <div className="grid grid-cols-7">
        {days.map((date) => {
          const key = toDateInput(date)
          const dayAppointments = appointments.filter((item) => toDateInput(new Date(item.appointmentDateTime)) === key)
          const inMonth = date.getMonth() === month.getMonth()
          const today = key === toDateInput()
          return (
            <div key={key} className={`min-h-28 border-b border-r border-slate-100 p-1.5 sm:min-h-36 sm:p-2 ${inMonth ? 'bg-white' : 'bg-slate-50/70'}`}>
              <div className={`mb-1 grid h-6 w-6 place-items-center rounded-full text-xs font-semibold ${today ? 'bg-sky-600 text-white' : inMonth ? 'text-slate-700' : 'text-slate-300'}`}>{date.getDate()}</div>
              <div className="space-y-1">
                {dayAppointments.slice(0, 3).map((item) => (
                  <Link key={item.id} to={`/appointments/${item.id}`} title={item.reason} className="block truncate rounded-md bg-sky-50 px-1.5 py-1 text-[10px] font-semibold text-sky-800 hover:bg-sky-100 sm:text-[11px]">
                    {timeOnly(item.appointmentDateTime)} <span className="hidden sm:inline">· {item.reason}</span>
                  </Link>
                ))}
                {dayAppointments.length > 3 && <p className="text-[10px] font-semibold text-slate-400">+{dayAppointments.length - 3} more</p>}
              </div>
            </div>
          )
        })}
      </div>
    </section>
  )
}

export function AppointmentListPage() {
  const { user } = useAuth()
  const [view, setView] = useState<ViewMode>('calendar')
  const [date, setDate] = useState('')
  const [status, setStatus] = useState<AppointmentStatus | ''>('')
  const [patientId, setPatientId] = useState('')
  const [doctorId, setDoctorId] = useState('')
  const [month, setMonth] = useState(() => new Date())
  const canManage = user?.role === 'ADMIN' || user?.role === 'RECEPTIONIST'
  const query = useQuery({
    queryKey: ['appointments', { view, date, status, patientId, doctorId }],
    queryFn: () => appointmentApi.list({
      date: view === 'list' && date ? date : undefined,
      status: status || undefined,
      patientId: patientId ? Number(patientId) : undefined,
      doctorId: doctorId ? Number(doctorId) : undefined,
    }),
  })

  return (
    <div className="space-y-7">
      <PageHeader eyebrow="Care schedule" title="Appointments" description="Coordinate visits, prevent scheduling conflicts, and keep every status change visible."
        actions={<>{user?.role === 'DOCTOR' && <Link to="/appointments/my-schedule" className="inline-flex items-center gap-2 rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-sm font-bold text-slate-700 hover:bg-slate-50"><Stethoscope className="h-4 w-4" /> My schedule</Link>}{canManage && <Link to="/appointments/new" className="inline-flex items-center gap-2 rounded-xl bg-sky-600 px-4 py-2.5 text-sm font-bold text-white shadow-lg shadow-sky-600/15 hover:bg-sky-700"><Plus className="h-4 w-4" /> New appointment</Link>}</>}
      />

      <section className="rounded-2xl border border-slate-200 bg-white p-4">
        <div className="flex flex-col gap-3 xl:flex-row xl:items-center xl:justify-between">
          <div className="flex rounded-xl bg-slate-100 p-1">
            <button type="button" onClick={() => setView('calendar')} className={`flex flex-1 items-center justify-center gap-2 rounded-lg px-4 py-2 text-sm font-semibold ${view === 'calendar' ? 'bg-white text-sky-700 shadow-sm' : 'text-slate-500'}`}><CalendarDays className="h-4 w-4" /> Calendar</button>
            <button type="button" onClick={() => setView('list')} className={`flex flex-1 items-center justify-center gap-2 rounded-lg px-4 py-2 text-sm font-semibold ${view === 'list' ? 'bg-white text-sky-700 shadow-sm' : 'text-slate-500'}`}><List className="h-4 w-4" /> List</button>
          </div>
          <div className="grid gap-2 sm:grid-cols-2 lg:grid-cols-4">
            {view === 'list' && <input type="date" value={date} onChange={(event) => setDate(event.target.value)} className="rounded-xl border border-slate-200 px-3 py-2.5 text-sm outline-none focus:border-sky-500" aria-label="Filter appointment date" />}
            <input type="number" min="1" value={patientId} onChange={(event) => setPatientId(event.target.value)} placeholder="Patient ID" className="rounded-xl border border-slate-200 px-3 py-2.5 text-sm outline-none focus:border-sky-500" />
            <input type="number" min="1" value={doctorId} onChange={(event) => setDoctorId(event.target.value)} placeholder="Doctor ID" className="rounded-xl border border-slate-200 px-3 py-2.5 text-sm outline-none focus:border-sky-500" />
            <select value={status} onChange={(event) => setStatus(event.target.value as AppointmentStatus | '')} className="rounded-xl border border-slate-200 px-3 py-2.5 text-sm outline-none focus:border-sky-500"><option value="">All statuses</option>{statuses.map((item) => <option key={item} value={item}>{item.replace('_', ' ')}</option>)}</select>
          </div>
        </div>
      </section>

      {query.isLoading ? <LoadingState label="Loading appointments" /> : query.isError ? <ErrorState message={query.error.message} onRetry={() => query.refetch()} /> : view === 'calendar' ? (
        <CalendarView appointments={query.data ?? []} month={month} changeMonth={(amount) => setMonth(new Date(month.getFullYear(), month.getMonth() + amount, 1))} />
      ) : (query.data?.length ?? 0) === 0 ? (
        <EmptyState title="No matching appointments" message="Change the filters or schedule a new appointment." action={canManage ? <Link to="/appointments/new" className="font-bold text-sky-700">Schedule appointment</Link> : undefined} />
      ) : (
        <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white">
          <div className="divide-y divide-slate-100">
            {query.data?.map((appointment) => (
              <Link key={appointment.id} to={`/appointments/${appointment.id}`} className="grid gap-3 p-4 hover:bg-sky-50/40 sm:grid-cols-[1.1fr_1fr_1fr_auto] sm:items-center sm:px-5">
                <div><p className="text-sm font-bold text-slate-900">{appointment.reason}</p><p className="mt-1 text-xs text-slate-500">Appointment #{appointment.id}</p></div>
                <div className="flex items-center gap-2 text-sm text-slate-600"><Clock3 className="h-4 w-4 text-sky-600" /> {formatDateTime(appointment.appointmentDateTime)} · {appointment.durationMinutes} min</div>
                <p className="text-sm text-slate-500">Patient {appointment.patientId} · Doctor {appointment.doctorId}</p>
                <AppointmentStatusBadge status={appointment.status} />
              </Link>
            ))}
          </div>
        </section>
      )}
    </div>
  )
}
