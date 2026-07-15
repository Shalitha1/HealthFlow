import { ShieldX } from 'lucide-react'
import { Link } from 'react-router-dom'

export function ForbiddenPage() {
  return (
    <div className="grid min-h-[70vh] place-items-center text-center">
      <div>
        <div className="mx-auto grid h-16 w-16 place-items-center rounded-2xl bg-amber-50 text-amber-700"><ShieldX className="h-7 w-7" /></div>
        <h1 className="mt-6 text-2xl font-bold text-slate-950">Access is restricted</h1>
        <p className="mt-2 text-sm text-slate-500">Your role does not allow access to this workspace.</p>
        <Link to="/dashboard" className="mt-6 inline-flex rounded-xl bg-sky-600 px-4 py-2.5 text-sm font-bold text-white hover:bg-sky-700">Return to dashboard</Link>
      </div>
    </div>
  )
}
