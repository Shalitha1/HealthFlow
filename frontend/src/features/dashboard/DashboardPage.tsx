import { useQuery } from '@tanstack/react-query'
import {
  Activity,
  ArrowUpRight,
  CalendarPlus,
  CircleCheck,
  Clock3,
  ShieldCheck,
  UserCheck,
  Users,
} from 'lucide-react'
import { Link } from 'react-router-dom'
import { auditApi } from '../../api/auditApi'
import { patientApi } from '../../api/patientApi'
import { systemApi } from '../../api/systemApi'
import { useAuth } from '../../auth/AuthProvider'
import { ErrorState } from '../../components/feedback/ErrorState'
import { LoadingState } from '../../components/feedback/LoadingState'
import { StatusBadge } from '../../components/feedback/StatusBadge'
import { PageHeader } from '../../components/layout/PageHeader'
import { formatDateTime, titleCase } from '../../utils/format'

export function DashboardPage() {
  const { user } = useAuth()
  const canViewPatients = user?.role === 'ADMIN' || user?.role === 'RECEPTIONIST'
  const canViewAudit = user?.role === 'ADMIN'

  const statistics = useQuery({
    queryKey: ['patient-statistics'],
    queryFn: patientApi.statistics,
    enabled: canViewPatients,
  })
  const recentPatients = useQuery({
    queryKey: ['patients', 'recent'],
    queryFn: () => patientApi.list({ page: 0, size: 5 }),
    enabled: canViewPatients,
  })
  const audit = useQuery({
    queryKey: ['audit-events'],
    queryFn: auditApi.list,
    enabled: canViewAudit,
  })
  const health = useQuery({
    queryKey: ['gateway-health'],
    queryFn: systemApi.gatewayHealth,
    refetchInterval: 60_000,
  })

  const firstName = user?.email.split('@')[0].split('.')[0] ?? 'there'

  return (
    <div className="space-y-8">
      <PageHeader
        eyebrow="Operations overview"
        title={`Good day, ${firstName.charAt(0).toUpperCase() + firstName.slice(1)}`}
        description="A focused view of patient activity, system health, and the work that needs your attention."
      />

      {canViewPatients ? (
        statistics.isLoading ? (
          <LoadingState label="Loading patient metrics" />
        ) : statistics.isError ? (
          <ErrorState message={statistics.error.message} onRetry={() => statistics.refetch()} />
        ) : (
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            {[
              { label: 'Total patients', value: statistics.data?.totalPatients ?? 0, icon: Users, tint: 'bg-sky-50 text-sky-700' },
              { label: 'Active patients', value: statistics.data?.activePatients ?? 0, icon: UserCheck, tint: 'bg-emerald-50 text-emerald-700' },
              { label: 'Inactive records', value: statistics.data?.inactivePatients ?? 0, icon: ShieldCheck, tint: 'bg-slate-100 text-slate-600' },
              { label: 'Registered this month', value: statistics.data?.registrationsThisMonth ?? 0, icon: CalendarPlus, tint: 'bg-violet-50 text-violet-700' },
            ].map((metric) => {
              const Icon = metric.icon
              return (
                <div key={metric.label} className="rounded-2xl border border-slate-200/80 bg-white p-5 shadow-sm shadow-slate-900/[0.02]">
                  <div className={`grid h-10 w-10 place-items-center rounded-xl ${metric.tint}`}><Icon className="h-5 w-5" /></div>
                  <div className="mt-5 text-3xl font-bold tracking-tight text-slate-950">{metric.value.toLocaleString()}</div>
                  <div className="mt-1 text-sm font-medium text-slate-500">{metric.label}</div>
                </div>
              )
            })}
          </div>
        )
      ) : (
        <div className="rounded-3xl border border-sky-100 bg-gradient-to-br from-sky-50 to-white p-7">
          <div className="grid h-11 w-11 place-items-center rounded-2xl bg-sky-600 text-white"><Activity className="h-5 w-5" /></div>
          <h2 className="mt-5 text-xl font-bold text-slate-950">Your clinical workspace is ready</h2>
          <p className="mt-2 max-w-2xl text-sm leading-6 text-slate-600">Patient directory access is scoped to your assigned records. Appointment workflows will appear here when the Appointment Service is introduced.</p>
        </div>
      )}

      <div className="grid gap-6 xl:grid-cols-[1.45fr_0.85fr]">
        <section className="overflow-hidden rounded-2xl border border-slate-200/80 bg-white">
          <div className="flex items-center justify-between border-b border-slate-100 px-5 py-4">
            <div>
              <h2 className="font-bold text-slate-900">{canViewPatients ? 'Recent registrations' : 'Recent activity'}</h2>
              <p className="mt-0.5 text-xs text-slate-500">Latest work across HealthFlow</p>
            </div>
            {canViewPatients && <Link to="/patients" className="flex items-center gap-1 text-xs font-bold text-sky-700 hover:text-sky-800">View all <ArrowUpRight className="h-3.5 w-3.5" /></Link>}
          </div>
          {canViewPatients ? (
            recentPatients.isLoading ? <LoadingState label="Loading registrations" /> : recentPatients.data?.patients.length ? (
              <div className="divide-y divide-slate-100">
                {recentPatients.data.patients.map((patient) => (
                  <Link key={patient.id} to={`/patients/${patient.id}`} className="flex items-center justify-between gap-4 px-5 py-4 hover:bg-slate-50/70">
                    <div className="flex min-w-0 items-center gap-3">
                      <div className="grid h-10 w-10 shrink-0 place-items-center rounded-xl bg-sky-50 text-sm font-bold text-sky-700">{patient.name.charAt(0)}</div>
                      <div className="min-w-0"><p className="truncate text-sm font-semibold text-slate-900">{patient.name}</p><p className="truncate text-xs text-slate-500">{patient.email}</p></div>
                    </div>
                    <StatusBadge active={patient.active} />
                  </Link>
                ))}
              </div>
            ) : <div className="p-8 text-center text-sm text-slate-500">No patient registrations yet.</div>
          ) : (
            <div className="p-8 text-center text-sm text-slate-500">Your assigned clinical activity will appear here.</div>
          )}
        </section>

        <div className="space-y-6">
          <section className="rounded-2xl border border-slate-200/80 bg-white p-5">
            <div className="flex items-center justify-between">
              <div><h2 className="font-bold text-slate-900">Service status</h2><p className="mt-0.5 text-xs text-slate-500">API Gateway</p></div>
              <div className={`flex items-center gap-2 rounded-full px-3 py-1.5 text-xs font-bold ${health.data?.status === 'UP' ? 'bg-emerald-50 text-emerald-700' : 'bg-amber-50 text-amber-700'}`}>
                <span className={`h-2 w-2 rounded-full ${health.data?.status === 'UP' ? 'bg-emerald-500' : 'bg-amber-500'}`} />
                {health.isLoading ? 'Checking' : health.data?.status === 'UP' ? 'Operational' : 'Unavailable'}
              </div>
            </div>
            <div className="mt-5 flex items-center gap-3 rounded-xl bg-slate-50 px-3.5 py-3 text-xs text-slate-600"><CircleCheck className="h-4 w-4 text-emerald-600" /> Health checks refresh automatically.</div>
          </section>

          {canViewAudit && (
            <section className="rounded-2xl border border-slate-200/80 bg-white p-5">
              <div className="flex items-center justify-between"><h2 className="font-bold text-slate-900">Recent audit events</h2><Link to="/audit" className="text-xs font-bold text-sky-700">View trail</Link></div>
              <div className="mt-4 space-y-3">
                {audit.data?.slice(0, 3).map((event) => (
                  <div key={event.id} className="flex gap-3">
                    <div className="mt-0.5 grid h-7 w-7 shrink-0 place-items-center rounded-lg bg-slate-100"><Clock3 className="h-3.5 w-3.5 text-slate-500" /></div>
                    <div className="min-w-0"><p className="truncate text-xs font-semibold text-slate-800">{titleCase(event.eventType)}</p><p className="mt-0.5 text-[11px] text-slate-400">{formatDateTime(event.timestamp)}</p></div>
                  </div>
                ))}
                {!audit.isLoading && !audit.data?.length && <p className="text-xs text-slate-500">No audit events yet.</p>}
              </div>
            </section>
          )}
        </div>
      </div>
    </div>
  )
}
