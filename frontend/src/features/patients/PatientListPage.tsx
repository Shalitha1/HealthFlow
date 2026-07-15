import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  ChevronLeft,
  ChevronRight,
  Eye,
  Pencil,
  Plus,
  Search,
  SlidersHorizontal,
  UserRoundCheck,
  UserRoundX,
} from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { patientApi } from '../../api/patientApi'
import { ApiRequestError } from '../../api/httpClient'
import { EmptyState } from '../../components/feedback/EmptyState'
import { ErrorState } from '../../components/feedback/ErrorState'
import { LoadingState } from '../../components/feedback/LoadingState'
import { StatusBadge } from '../../components/feedback/StatusBadge'
import { PageHeader } from '../../components/layout/PageHeader'
import { formatDate } from '../../utils/format'

type StatusFilter = 'all' | 'active' | 'inactive'

export function PatientListPage() {
  const [searchInput, setSearchInput] = useState('')
  const [search, setSearch] = useState('')
  const [status, setStatus] = useState<StatusFilter>('all')
  const [page, setPage] = useState(0)
  const queryClient = useQueryClient()

  useEffect(() => {
    const timeout = window.setTimeout(() => {
      setSearch(searchInput.trim())
      setPage(0)
    }, 350)
    return () => window.clearTimeout(timeout)
  }, [searchInput])

  const active = status === 'all' ? undefined : status === 'active'
  const patients = useQuery({
    queryKey: ['patients', { page, search, active }],
    queryFn: () => patientApi.list({ page, size: 10, search, active }),
    placeholderData: keepPreviousData,
  })

  const statusMutation = useMutation({
    mutationFn: ({ id, nextActive }: { id: number; nextActive: boolean }) =>
      patientApi.updateStatus(id, nextActive),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['patients'] })
      queryClient.invalidateQueries({ queryKey: ['patient-statistics'] })
    },
  })
  const pageData = patients.data

  return (
    <div className="space-y-7">
      <PageHeader
        eyebrow="Patient directory"
        title="Patients"
        description="Search the complete patient history, manage active status, and open a record for clinical context."
        actions={
          <Link to="/patients/new" className="inline-flex items-center gap-2 rounded-xl bg-sky-600 px-4 py-2.5 text-sm font-bold text-white shadow-lg shadow-sky-600/15 hover:bg-sky-700">
            <Plus className="h-4 w-4" /> New patient
          </Link>
        }
      />

      <section className="overflow-hidden rounded-2xl border border-slate-200/80 bg-white shadow-sm shadow-slate-900/[0.02]">
        <div className="flex flex-col gap-3 border-b border-slate-100 p-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="relative w-full sm:max-w-md">
            <Search className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
            <input
              value={searchInput}
              onChange={(event) => setSearchInput(event.target.value)}
              placeholder="Search name, email, or patient ID"
              className="w-full rounded-xl border border-slate-200 bg-slate-50/60 py-2.5 pl-10 pr-4 text-sm outline-none transition focus:border-sky-500 focus:bg-white focus:ring-4 focus:ring-sky-100"
              aria-label="Search patients"
            />
          </div>
          <div className="flex items-center gap-2">
            <SlidersHorizontal className="hidden h-4 w-4 text-slate-400 sm:block" />
            <select
              value={status}
              onChange={(event) => {
                setStatus(event.target.value as StatusFilter)
                setPage(0)
              }}
              className="w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm font-medium text-slate-700 outline-none focus:border-sky-500 focus:ring-4 focus:ring-sky-100 sm:w-44"
              aria-label="Filter by patient status"
            >
              <option value="all">All statuses</option>
              <option value="active">Active</option>
              <option value="inactive">Inactive</option>
            </select>
          </div>
        </div>

        {patients.isLoading || !pageData ? (
          <LoadingState label="Loading patient records" />
        ) : patients.isError ? (
          <div className="p-5"><ErrorState message={patients.error.message} onRetry={() => patients.refetch()} /></div>
        ) : pageData.patients.length === 0 ? (
          <div className="p-5">
            <EmptyState
              title={search || status !== 'all' ? 'No matching patients' : 'No patients yet'}
              message={search || status !== 'all' ? 'Try a different search or status filter.' : 'Create the first patient record to begin.'}
              action={!search && status === 'all' ? <Link to="/patients/new" className="text-sm font-bold text-sky-700">Create patient</Link> : undefined}
            />
          </div>
        ) : (
          <>
            <div className="hidden overflow-x-auto md:block">
              <table className="w-full text-left">
                <thead className="bg-slate-50/80 text-[11px] font-bold uppercase tracking-[0.12em] text-slate-500">
                  <tr><th className="px-5 py-3.5">Patient</th><th className="px-4 py-3.5">Patient ID</th><th className="px-4 py-3.5">Registered</th><th className="px-4 py-3.5">Status</th><th className="px-5 py-3.5 text-right">Actions</th></tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {pageData.patients.map((patient) => (
                    <tr key={patient.id} className="group hover:bg-sky-50/30">
                      <td className="px-5 py-4"><div className="flex items-center gap-3"><div className="grid h-10 w-10 place-items-center rounded-xl bg-sky-50 text-sm font-bold text-sky-700">{patient.name.charAt(0).toUpperCase()}</div><div><Link to={`/patients/${patient.id}`} className="text-sm font-semibold text-slate-900 hover:text-sky-700">{patient.name}</Link><p className="mt-0.5 text-xs text-slate-500">{patient.email}</p></div></div></td>
                      <td className="px-4 py-4 text-sm font-medium text-slate-600">HF-{String(patient.id).padStart(5, '0')}</td>
                      <td className="px-4 py-4 text-sm text-slate-500">{formatDate(patient.createdAt)}</td>
                      <td className="px-4 py-4"><StatusBadge active={patient.active} /></td>
                      <td className="px-5 py-4"><div className="flex justify-end gap-1"><Link to={`/patients/${patient.id}`} className="rounded-lg p-2 text-slate-400 hover:bg-sky-50 hover:text-sky-700" aria-label={`View ${patient.name}`}><Eye className="h-4 w-4" /></Link><Link to={`/patients/${patient.id}/edit`} className="rounded-lg p-2 text-slate-400 hover:bg-sky-50 hover:text-sky-700" aria-label={`Edit ${patient.name}`}><Pencil className="h-4 w-4" /></Link><button type="button" disabled={statusMutation.isPending} onClick={() => statusMutation.mutate({ id: patient.id, nextActive: !patient.active })} className="rounded-lg p-2 text-slate-400 hover:bg-slate-100 hover:text-slate-700" aria-label={`${patient.active ? 'Deactivate' : 'Reactivate'} ${patient.name}`}>{patient.active ? <UserRoundX className="h-4 w-4" /> : <UserRoundCheck className="h-4 w-4" />}</button></div></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <div className="divide-y divide-slate-100 md:hidden">
              {pageData.patients.map((patient) => (
                <div key={patient.id} className="p-4">
                  <div className="flex items-start justify-between gap-3"><div className="min-w-0"><Link to={`/patients/${patient.id}`} className="truncate text-sm font-bold text-slate-900">{patient.name}</Link><p className="mt-1 truncate text-xs text-slate-500">{patient.email}</p></div><StatusBadge active={patient.active} /></div>
                  <div className="mt-4 flex items-center justify-between"><div className="text-xs text-slate-500"><span className="font-semibold text-slate-700">HF-{String(patient.id).padStart(5, '0')}</span> · {formatDate(patient.createdAt)}</div><div className="flex gap-1"><Link to={`/patients/${patient.id}`} className="rounded-lg p-2 text-sky-700"><Eye className="h-4 w-4" /></Link><Link to={`/patients/${patient.id}/edit`} className="rounded-lg p-2 text-sky-700"><Pencil className="h-4 w-4" /></Link></div></div>
                </div>
              ))}
            </div>

            <div className="flex flex-col gap-3 border-t border-slate-100 px-4 py-3.5 sm:flex-row sm:items-center sm:justify-between">
              <p className="text-xs text-slate-500">Page <span className="font-semibold text-slate-700">{pageData.currentPage + 1}</span> of <span className="font-semibold text-slate-700">{Math.max(pageData.totalPages, 1)}</span> · {pageData.totalElements} records</p>
              <div className="flex items-center gap-2"><button type="button" onClick={() => setPage((current) => Math.max(0, current - 1))} disabled={page === 0} className="inline-flex items-center gap-1 rounded-lg border border-slate-200 px-3 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-40"><ChevronLeft className="h-4 w-4" /> Previous</button><button type="button" onClick={() => setPage((current) => current + 1)} disabled={page + 1 >= pageData.totalPages} className="inline-flex items-center gap-1 rounded-lg border border-slate-200 px-3 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-40">Next <ChevronRight className="h-4 w-4" /></button></div>
            </div>
          </>
        )}

        {statusMutation.isError && (
          <div className="border-t border-rose-100 bg-rose-50 px-4 py-3 text-xs font-medium text-rose-700">
            {statusMutation.error instanceof ApiRequestError ? statusMutation.error.message : 'Could not update patient status.'}
          </div>
        )}
      </section>
    </div>
  )
}
