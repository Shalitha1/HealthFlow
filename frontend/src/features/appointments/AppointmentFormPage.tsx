import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ArrowLeft, CalendarPlus, Save } from 'lucide-react'
import { useEffect, useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { z } from 'zod'
import { appointmentApi } from '../../api/appointmentApi'
import { ApiRequestError } from '../../api/httpClient'
import { patientApi } from '../../api/patientApi'
import { ErrorState } from '../../components/feedback/ErrorState'
import { LoadingState } from '../../components/feedback/LoadingState'
import { PageHeader } from '../../components/layout/PageHeader'
import { dateTimeForApi, toDateTimeInput } from './appointmentUtils'

const schema = z.object({
  patientId: z.number().int().positive('Select a patient'),
  doctorId: z.number().int().positive('Enter a valid doctor ID'),
  appointmentDateTime: z.string().min(1, 'Date and time are required').refine((value) => new Date(value) > new Date(), 'Appointment must be in the future'),
  durationMinutes: z.number().int().min(5, 'Minimum duration is 5 minutes').max(480, 'Maximum duration is 480 minutes'),
  reason: z.string().trim().min(2, 'Enter the reason for this appointment').max(500),
  notes: z.string().max(5000).optional(),
})
type FormValues = z.infer<typeof schema>

export function AppointmentFormPage() {
  const { id } = useParams()
  const editing = Boolean(id)
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [serverError, setServerError] = useState<string | null>(null)
  const tomorrow = new Date(Date.now() + 24 * 60 * 60 * 1000)
  tomorrow.setHours(9, 0, 0, 0)

  const appointment = useQuery({ queryKey: ['appointment', id], queryFn: () => appointmentApi.get(id!), enabled: editing })
  const patients = useQuery({ queryKey: ['patients', 'appointment-picker'], queryFn: () => patientApi.list({ page: 0, size: 100, active: true }) })
  const { register, handleSubmit, reset, setError, formState: { errors } } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { patientId: 0, doctorId: 0, appointmentDateTime: toDateTimeInput(tomorrow), durationMinutes: 30, reason: '', notes: '' },
  })

  useEffect(() => {
    if (appointment.data) reset({
      patientId: appointment.data.patientId,
      doctorId: appointment.data.doctorId,
      appointmentDateTime: toDateTimeInput(appointment.data.appointmentDateTime),
      durationMinutes: appointment.data.durationMinutes,
      reason: appointment.data.reason,
      notes: appointment.data.notes ?? '',
    })
  }, [appointment.data, reset])

  const mutation = useMutation({
    mutationFn: (values: FormValues) => {
      const request = { ...values, appointmentDateTime: dateTimeForApi(values.appointmentDateTime) }
      return editing ? appointmentApi.update(id!, request) : appointmentApi.create(request)
    },
    onSuccess: (saved) => {
      queryClient.invalidateQueries({ queryKey: ['appointments'] })
      queryClient.setQueryData(['appointment', String(saved.id)], saved)
      navigate(`/appointments/${saved.id}`, { replace: true })
    },
    onError: (error) => {
      if (error instanceof ApiRequestError) {
        setServerError(error.message)
        Object.entries(error.fieldErrors).forEach(([field, message]) => {
          if (field in schema.shape) setError(field as keyof FormValues, { message })
        })
      } else setServerError('The appointment could not be saved.')
    },
  })

  if (appointment.isLoading) return <LoadingState label="Loading appointment" />
  if (appointment.isError) return <ErrorState message={appointment.error.message} onRetry={() => appointment.refetch()} />
  const input = 'mt-2 w-full rounded-xl border border-slate-200 bg-white px-3.5 py-3 text-sm outline-none focus:border-sky-500 focus:ring-4 focus:ring-sky-100'

  return (
    <div className="mx-auto max-w-4xl space-y-7">
      <PageHeader eyebrow="Care schedule" title={editing ? 'Reschedule appointment' : 'Schedule an appointment'} description="Choose an available time for both the patient and doctor. Overlapping appointments are rejected automatically."
        actions={<Link to={editing ? `/appointments/${id}` : '/appointments'} className="inline-flex items-center gap-2 rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-sm font-semibold text-slate-700 hover:bg-slate-50"><ArrowLeft className="h-4 w-4" /> Cancel</Link>} />

      <form onSubmit={handleSubmit((values) => { setServerError(null); mutation.mutate(values) })} className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm sm:p-7" noValidate>
        <div className="mb-6 flex items-center gap-3 rounded-2xl bg-sky-50 p-4 text-sm text-sky-800"><CalendarPlus className="h-5 w-5 shrink-0" /> Times are shown in your local timezone and saved as an exact timestamp.</div>
        <div className="grid gap-6 sm:grid-cols-2">
          <div><label htmlFor="patientId" className="text-sm font-semibold text-slate-700">Patient</label><select id="patientId" className={input} {...register('patientId', { valueAsNumber: true })}><option value={0}>Select a patient</option>{patients.data?.patients.map((patient) => <option key={patient.id} value={patient.id}>{patient.name} · HF-{String(patient.id).padStart(5, '0')}</option>)}</select>{errors.patientId && <p className="mt-1.5 text-xs font-medium text-rose-600">{errors.patientId.message}</p>}</div>
          <div><label htmlFor="doctorId" className="text-sm font-semibold text-slate-700">Doctor ID</label><input id="doctorId" type="number" min="1" placeholder="e.g. 3" className={input} {...register('doctorId', { valueAsNumber: true })} />{errors.doctorId && <p className="mt-1.5 text-xs font-medium text-rose-600">{errors.doctorId.message}</p>}</div>
          <div><label htmlFor="appointmentDateTime" className="text-sm font-semibold text-slate-700">Date and time</label><input id="appointmentDateTime" type="datetime-local" className={input} {...register('appointmentDateTime')} />{errors.appointmentDateTime && <p className="mt-1.5 text-xs font-medium text-rose-600">{errors.appointmentDateTime.message}</p>}</div>
          <div><label htmlFor="durationMinutes" className="text-sm font-semibold text-slate-700">Duration</label><select id="durationMinutes" className={input} {...register('durationMinutes', { valueAsNumber: true })}>{[15, 30, 45, 60, 90, 120].map((minutes) => <option key={minutes} value={minutes}>{minutes} minutes</option>)}</select>{errors.durationMinutes && <p className="mt-1.5 text-xs font-medium text-rose-600">{errors.durationMinutes.message}</p>}</div>
          <div className="sm:col-span-2"><label htmlFor="reason" className="text-sm font-semibold text-slate-700">Reason</label><input id="reason" placeholder="Consultation, follow-up, annual review…" className={input} {...register('reason')} />{errors.reason && <p className="mt-1.5 text-xs font-medium text-rose-600">{errors.reason.message}</p>}</div>
          <div className="sm:col-span-2"><label htmlFor="notes" className="text-sm font-semibold text-slate-700">Notes <span className="font-normal text-slate-400">(optional)</span></label><textarea id="notes" rows={4} placeholder="Preparation or scheduling notes" className={`${input} resize-y`} {...register('notes')} />{errors.notes && <p className="mt-1.5 text-xs font-medium text-rose-600">{errors.notes.message}</p>}</div>
        </div>
        {patients.isError && <p className="mt-5 text-sm text-amber-700">Patient choices could not be loaded. You can retry from the Patients page.</p>}
        {serverError && <div className="mt-6 rounded-xl border border-rose-100 bg-rose-50 px-4 py-3 text-sm text-rose-700" role="alert">{serverError}</div>}
        <div className="mt-7 flex justify-end border-t border-slate-100 pt-5"><button type="submit" disabled={mutation.isPending} className="inline-flex min-w-44 items-center justify-center gap-2 rounded-xl bg-sky-600 px-5 py-3 text-sm font-bold text-white shadow-lg shadow-sky-600/15 hover:bg-sky-700 disabled:opacity-60"><Save className="h-4 w-4" /> {mutation.isPending ? 'Saving…' : editing ? 'Save new time' : 'Schedule appointment'}</button></div>
      </form>
    </div>
  )
}
