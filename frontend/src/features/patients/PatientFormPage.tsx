import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ArrowLeft, Save } from 'lucide-react'
import { useEffect, useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { z } from 'zod'
import { ApiRequestError } from '../../api/httpClient'
import { patientApi } from '../../api/patientApi'
import { ErrorState } from '../../components/feedback/ErrorState'
import { LoadingState } from '../../components/feedback/LoadingState'
import { PageHeader } from '../../components/layout/PageHeader'

const patientSchema = z.object({
  name: z.string().trim().min(2, 'Enter the patient name').max(255),
  email: z.string().trim().email('Enter a valid email address').max(255),
  address: z.string().trim().min(3, 'Enter the patient address').max(500),
  dateOfBirth: z
    .string()
    .min(1, 'Date of birth is required')
    .refine((value) => new Date(value) < new Date(), 'Date of birth must be in the past'),
})

type PatientFormValues = z.infer<typeof patientSchema>

export function PatientFormPage() {
  const { id } = useParams()
  const isEditing = Boolean(id)
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [serverError, setServerError] = useState<string | null>(null)
  const patient = useQuery({
    queryKey: ['patient', id],
    queryFn: () => patientApi.get(id!),
    enabled: isEditing,
  })
  const {
    register,
    handleSubmit,
    reset,
    setError,
    formState: { errors },
  } = useForm<PatientFormValues>({
    resolver: zodResolver(patientSchema),
    defaultValues: { name: '', email: '', address: '', dateOfBirth: '' },
  })

  useEffect(() => {
    if (patient.data) {
      reset({
        name: patient.data.name,
        email: patient.data.email,
        address: patient.data.address,
        dateOfBirth: patient.data.dateOfBirth,
      })
    }
  }, [patient.data, reset])

  const mutation = useMutation({
    mutationFn: (values: PatientFormValues) =>
      isEditing ? patientApi.update(id!, values) : patientApi.create(values),
    onSuccess: (saved) => {
      queryClient.invalidateQueries({ queryKey: ['patients'] })
      queryClient.invalidateQueries({ queryKey: ['patient-statistics'] })
      queryClient.setQueryData(['patient', String(saved.id)], saved)
      navigate(`/patients/${saved.id}`, { replace: true })
    },
    onError: (error) => {
      if (error instanceof ApiRequestError) {
        setServerError(error.message)
        Object.entries(error.fieldErrors).forEach(([field, message]) => {
          if (field in patientSchema.shape) {
            setError(field as keyof PatientFormValues, { message })
          }
        })
      } else {
        setServerError('The patient record could not be saved.')
      }
    },
  })

  if (patient.isLoading) return <LoadingState label="Loading patient record" />
  if (patient.isError) return <ErrorState message={patient.error.message} onRetry={() => patient.refetch()} />

  const inputClass = 'mt-2 w-full rounded-xl border border-slate-200 bg-white px-3.5 py-3 text-sm outline-none transition placeholder:text-slate-400 focus:border-sky-500 focus:ring-4 focus:ring-sky-100'

  return (
    <div className="mx-auto max-w-4xl space-y-7">
      <PageHeader
        eyebrow="Patient record"
        title={isEditing ? 'Edit patient' : 'Register a patient'}
        description={isEditing ? 'Update demographic and contact information. Status is managed from the patient record.' : 'Create a validated patient record and begin the HealthFlow journey.'}
        actions={<Link to={isEditing ? `/patients/${id}` : '/patients'} className="inline-flex items-center gap-2 rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-sm font-semibold text-slate-700 hover:bg-slate-50"><ArrowLeft className="h-4 w-4" /> Cancel</Link>}
      />

      <form onSubmit={handleSubmit((values) => { setServerError(null); mutation.mutate(values) })} className="rounded-2xl border border-slate-200/80 bg-white p-5 shadow-sm sm:p-7" noValidate>
        <div className="grid gap-6 sm:grid-cols-2">
          <div className="sm:col-span-2"><label htmlFor="name" className="text-sm font-semibold text-slate-700">Full name</label><input id="name" placeholder="Patient's full name" className={inputClass} {...register('name')} />{errors.name && <p className="mt-1.5 text-xs font-medium text-rose-600">{errors.name.message}</p>}</div>
          <div><label htmlFor="email" className="text-sm font-semibold text-slate-700">Email address</label><input id="email" type="email" placeholder="patient@example.com" className={inputClass} {...register('email')} />{errors.email && <p className="mt-1.5 text-xs font-medium text-rose-600">{errors.email.message}</p>}</div>
          <div><label htmlFor="dateOfBirth" className="text-sm font-semibold text-slate-700">Date of birth</label><input id="dateOfBirth" type="date" className={inputClass} {...register('dateOfBirth')} />{errors.dateOfBirth && <p className="mt-1.5 text-xs font-medium text-rose-600">{errors.dateOfBirth.message}</p>}</div>
          <div className="sm:col-span-2"><label htmlFor="address" className="text-sm font-semibold text-slate-700">Address</label><textarea id="address" rows={4} placeholder="Residential address" className={`${inputClass} resize-y`} {...register('address')} />{errors.address && <p className="mt-1.5 text-xs font-medium text-rose-600">{errors.address.message}</p>}</div>
        </div>

        {serverError && <div className="mt-6 rounded-xl border border-rose-100 bg-rose-50 px-4 py-3 text-sm text-rose-700" role="alert">{serverError}</div>}

        <div className="mt-7 flex justify-end border-t border-slate-100 pt-5">
          <button type="submit" disabled={mutation.isPending} className="inline-flex min-w-36 items-center justify-center gap-2 rounded-xl bg-sky-600 px-5 py-3 text-sm font-bold text-white shadow-lg shadow-sky-600/15 hover:bg-sky-700 disabled:opacity-60"><Save className="h-4 w-4" /> {mutation.isPending ? 'Saving…' : isEditing ? 'Save changes' : 'Create patient'}</button>
        </div>
      </form>
    </div>
  )
}
