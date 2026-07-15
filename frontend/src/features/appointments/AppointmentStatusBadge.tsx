import type { AppointmentStatus } from '../../types/api'
import { titleCase } from '../../utils/format'

const colors: Record<AppointmentStatus, string> = {
  SCHEDULED: 'bg-sky-50 text-sky-700 ring-sky-200',
  CONFIRMED: 'bg-violet-50 text-violet-700 ring-violet-200',
  COMPLETED: 'bg-emerald-50 text-emerald-700 ring-emerald-200',
  CANCELLED: 'bg-rose-50 text-rose-700 ring-rose-200',
  NO_SHOW: 'bg-amber-50 text-amber-700 ring-amber-200',
}

export function AppointmentStatusBadge({ status }: { status: AppointmentStatus }) {
  return <span className={`inline-flex rounded-full px-2.5 py-1 text-[11px] font-bold ring-1 ring-inset ${colors[status]}`}>{titleCase(status)}</span>
}
