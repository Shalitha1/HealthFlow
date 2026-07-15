import { LoaderCircle } from 'lucide-react'

export function LoadingState({
  label = 'Loading',
  fullScreen = false,
}: {
  label?: string
  fullScreen?: boolean
}) {
  return (
    <div
      className={`flex items-center justify-center ${fullScreen ? 'min-h-screen bg-slate-50' : 'min-h-56'}`}
      role="status"
    >
      <div className="flex flex-col items-center gap-3 text-sm font-medium text-slate-500">
        <LoaderCircle className="h-7 w-7 animate-spin text-sky-600" />
        <span>{label}</span>
      </div>
    </div>
  )
}
