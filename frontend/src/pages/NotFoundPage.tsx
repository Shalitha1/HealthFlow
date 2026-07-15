import { Link } from 'react-router-dom'

export function NotFoundPage() {
  return (
    <div className="grid min-h-screen place-items-center bg-slate-50 px-6 text-center">
      <div><p className="text-sm font-bold uppercase tracking-[0.25em] text-sky-600">404</p><h1 className="mt-3 text-3xl font-bold text-slate-950">Page not found</h1><p className="mt-2 text-sm text-slate-500">The page you requested does not exist.</p><Link to="/" className="mt-6 inline-flex rounded-xl bg-sky-600 px-4 py-2.5 text-sm font-bold text-white">Back to HealthFlow</Link></div>
    </div>
  )
}
