import { useQuery } from '@tanstack/react-query'
import { ChevronDown, Filter, Search, ShieldCheck } from 'lucide-react'
import { useMemo, useState } from 'react'
import { auditApi } from '../../api/auditApi'
import { EmptyState } from '../../components/feedback/EmptyState'
import { ErrorState } from '../../components/feedback/ErrorState'
import { LoadingState } from '../../components/feedback/LoadingState'
import { PageHeader } from '../../components/layout/PageHeader'
import { formatDateTime, titleCase } from '../../utils/format'

export function AuditPage() {
  const [search, setSearch] = useState('')
  const [eventType, setEventType] = useState('all')
  const audit = useQuery({ queryKey: ['audit-events'], queryFn: auditApi.list })
  const eventTypes = useMemo(() => [...new Set(audit.data?.map((event) => event.eventType) ?? [])].sort(), [audit.data])
  const filtered = useMemo(() => {
    const term = search.trim().toLowerCase()
    return (audit.data ?? []).filter((event) => {
      const typeMatches = eventType === 'all' || event.eventType === eventType
      const searchMatches = !term || event.eventType.toLowerCase().includes(term) || event.actorId?.toLowerCase().includes(term) || event.payload.toLowerCase().includes(term)
      return typeMatches && searchMatches
    })
  }, [audit.data, eventType, search])

  return (
    <div className="space-y-7">
      <PageHeader eyebrow="Security & compliance" title="Audit trail" description="Review immutable patient activity events and inspect their original payloads." />
      <section className="overflow-hidden rounded-2xl border border-slate-200/80 bg-white">
        <div className="flex flex-col gap-3 border-b border-slate-100 p-4 sm:flex-row">
          <div className="relative flex-1"><Search className="absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" /><input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Search event, actor, or payload" className="w-full rounded-xl border border-slate-200 bg-slate-50/60 py-2.5 pl-10 pr-4 text-sm outline-none focus:border-sky-500 focus:bg-white focus:ring-4 focus:ring-sky-100" /></div>
          <div className="relative sm:w-60"><Filter className="absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" /><select value={eventType} onChange={(event) => setEventType(event.target.value)} className="w-full appearance-none rounded-xl border border-slate-200 bg-white py-2.5 pl-10 pr-9 text-sm font-medium outline-none focus:border-sky-500 focus:ring-4 focus:ring-sky-100"><option value="all">All event types</option>{eventTypes.map((type) => <option key={type} value={type}>{titleCase(type)}</option>)}</select><ChevronDown className="pointer-events-none absolute right-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" /></div>
        </div>
        {audit.isLoading ? <LoadingState label="Loading audit trail" /> : audit.isError ? <div className="p-5"><ErrorState message={audit.error.message} onRetry={() => audit.refetch()} /></div> : filtered.length === 0 ? <div className="p-5"><EmptyState title="No audit events found" message="Activity events will appear here as patient workflows occur." /></div> : <div className="divide-y divide-slate-100">{filtered.map((event) => <details key={event.id} className="group px-4 py-4 sm:px-5"><summary className="flex cursor-pointer list-none items-start justify-between gap-4"><div className="flex min-w-0 items-start gap-3"><div className="grid h-9 w-9 shrink-0 place-items-center rounded-xl bg-sky-50 text-sky-700"><ShieldCheck className="h-4 w-4" /></div><div className="min-w-0"><p className="truncate text-sm font-semibold text-slate-900">{titleCase(event.eventType)}</p><p className="mt-1 text-xs text-slate-500">Actor {event.actorId ?? 'System'} · {formatDateTime(event.timestamp)}</p></div></div><ChevronDown className="mt-2 h-4 w-4 shrink-0 text-slate-400 transition group-open:rotate-180" /></summary><div className="ml-12 mt-3 rounded-xl bg-slate-950 p-4"><pre className="overflow-x-auto whitespace-pre-wrap break-words text-xs leading-6 text-slate-200">{event.payload}</pre></div></details>)}</div>}
      </section>
    </div>
  )
}
