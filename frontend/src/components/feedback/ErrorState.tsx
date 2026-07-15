import { CircleAlert, RefreshCw } from 'lucide-react'

export function ErrorState({
  title = 'Something went wrong',
  message,
  onRetry,
}: {
  title?: string
  message: string
  onRetry?: () => void
}) {
  return (
    <div className="rounded-2xl border border-rose-100 bg-rose-50/70 p-6 text-center">
      <CircleAlert className="mx-auto h-8 w-8 text-rose-500" />
      <h3 className="mt-3 font-semibold text-slate-900">{title}</h3>
      <p className="mx-auto mt-1 max-w-xl text-sm text-slate-600">{message}</p>
      {onRetry && (
        <button
          type="button"
          onClick={onRetry}
          className="mt-4 inline-flex items-center gap-2 rounded-xl border border-rose-200 bg-white px-4 py-2 text-sm font-semibold text-rose-700 hover:bg-rose-50"
        >
          <RefreshCw className="h-4 w-4" /> Retry
        </button>
      )}
    </div>
  )
}
