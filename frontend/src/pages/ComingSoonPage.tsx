import { CalendarClock, Construction } from 'lucide-react'
import { PageHeader } from '../components/layout/PageHeader'

export function ComingSoonPage({ title, description }: { title: string; description: string }) {
  return (
    <div className="space-y-8">
      <PageHeader eyebrow="Future module" title={title} description={description} />
      <div className="flex min-h-96 flex-col items-center justify-center rounded-3xl border border-dashed border-slate-200 bg-white px-6 text-center">
        <div className="relative grid h-16 w-16 place-items-center rounded-2xl bg-sky-50 text-sky-700">
          <Construction className="h-7 w-7" />
          <CalendarClock className="absolute -bottom-1 -right-1 h-6 w-6 rounded-lg bg-white p-1 text-slate-500 shadow" />
        </div>
        <h2 className="mt-6 text-xl font-bold text-slate-950">Designed for the next milestone</h2>
        <p className="mt-2 max-w-lg text-sm leading-6 text-slate-500">The navigation and responsive workspace are ready. This module will connect as soon as its backend read APIs are available.</p>
      </div>
    </div>
  )
}
