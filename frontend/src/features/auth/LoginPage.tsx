import { zodResolver } from '@hookform/resolvers/zod'
import { Activity, ArrowRight, Eye, EyeOff, HeartPulse, LockKeyhole } from 'lucide-react'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import { z } from 'zod'
import { ApiRequestError } from '../../api/httpClient'
import { useAuth } from '../../auth/AuthProvider'

const loginSchema = z.object({
  email: z.string().trim().min(1, 'Email is required').email('Enter a valid email'),
  password: z.string().min(1, 'Password is required'),
})

type LoginForm = z.infer<typeof loginSchema>

export function LoginPage() {
  const [showPassword, setShowPassword] = useState(false)
  const [serverError, setServerError] = useState<string | null>(null)
  const { user, login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginForm>({ resolver: zodResolver(loginSchema) })

  if (user) return <Navigate to="/dashboard" replace />

  const onSubmit = async (values: LoginForm) => {
    setServerError(null)
    try {
      await login(values)
      const state = location.state as { from?: string } | null
      navigate(state?.from ?? '/dashboard', { replace: true })
    } catch (error) {
      setServerError(
        error instanceof ApiRequestError
          ? error.message
          : 'We could not sign you in. Please try again.',
      )
    }
  }

  return (
    <div className="grid min-h-screen bg-white lg:grid-cols-[1.05fr_0.95fr]">
      <section className="relative hidden overflow-hidden bg-sky-700 p-12 text-white lg:flex lg:flex-col lg:justify-between">
        <div className="absolute -left-24 top-20 h-96 w-96 rounded-full bg-cyan-300/20 blur-3xl" />
        <div className="absolute -bottom-32 right-0 h-[30rem] w-[30rem] rounded-full bg-blue-950/30 blur-3xl" />
        <div className="relative flex items-center gap-3">
          <div className="grid h-11 w-11 place-items-center rounded-2xl bg-white/15 ring-1 ring-white/25">
            <Activity className="h-6 w-6" />
          </div>
          <div>
            <div className="text-lg font-bold">HealthFlow</div>
            <div className="text-xs font-medium text-sky-100">Clinical operations, connected.</div>
          </div>
        </div>

        <div className="relative max-w-xl">
          <div className="mb-7 inline-flex items-center gap-2 rounded-full border border-white/20 bg-white/10 px-3 py-1.5 text-xs font-semibold text-sky-50 backdrop-blur">
            <HeartPulse className="h-4 w-4" /> Care teams move faster together
          </div>
          <h1 className="text-5xl font-bold leading-[1.08] tracking-tight">
            One calm workspace for every patient journey.
          </h1>
          <p className="mt-6 max-w-lg text-base leading-7 text-sky-100">
            Keep patient records, operational insight, and clinical activity close—without losing sight of the person behind the data.
          </p>
        </div>

        <div className="relative grid grid-cols-3 gap-3 text-sm">
          {['Secure access', 'Clear workflows', 'Live service data'].map((item) => (
            <div key={item} className="rounded-2xl border border-white/15 bg-white/10 px-4 py-3 text-sky-50 backdrop-blur">
              {item}
            </div>
          ))}
        </div>
      </section>

      <section className="flex items-center justify-center bg-slate-50/50 px-5 py-10 sm:px-10">
        <div className="w-full max-w-md">
          <div className="mb-10 flex items-center gap-3 lg:hidden">
            <div className="grid h-10 w-10 place-items-center rounded-2xl bg-sky-600 text-white">
              <Activity className="h-5 w-5" />
            </div>
            <span className="font-bold text-slate-950">HealthFlow</span>
          </div>

          <div className="rounded-3xl border border-slate-200/80 bg-white p-6 shadow-xl shadow-slate-900/[0.04] sm:p-9">
            <div className="grid h-12 w-12 place-items-center rounded-2xl bg-sky-50 text-sky-700">
              <LockKeyhole className="h-6 w-6" />
            </div>
            <h2 className="mt-6 text-3xl font-bold tracking-tight text-slate-950">Welcome back</h2>
            <p className="mt-2 text-sm leading-6 text-slate-500">
              Sign in with your HealthFlow staff account.
            </p>

            <form onSubmit={handleSubmit(onSubmit)} className="mt-8 space-y-5" noValidate>
              <div>
                <label htmlFor="email" className="text-sm font-semibold text-slate-700">Email address</label>
                <input
                  id="email"
                  type="email"
                  autoComplete="email"
                  placeholder="you@healthflow.com"
                  className="mt-2 w-full rounded-xl border border-slate-200 bg-white px-3.5 py-3 text-sm outline-none transition placeholder:text-slate-400 focus:border-sky-500 focus:ring-4 focus:ring-sky-100"
                  {...register('email')}
                />
                {errors.email && <p className="mt-1.5 text-xs font-medium text-rose-600">{errors.email.message}</p>}
              </div>

              <div>
                <label htmlFor="password" className="text-sm font-semibold text-slate-700">Password</label>
                <div className="relative mt-2">
                  <input
                    id="password"
                    type={showPassword ? 'text' : 'password'}
                    autoComplete="current-password"
                    placeholder="Enter your password"
                    className="w-full rounded-xl border border-slate-200 bg-white px-3.5 py-3 pr-11 text-sm outline-none transition placeholder:text-slate-400 focus:border-sky-500 focus:ring-4 focus:ring-sky-100"
                    {...register('password')}
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword((visible) => !visible)}
                    className="absolute inset-y-0 right-0 grid w-11 place-items-center text-slate-400 hover:text-slate-700"
                    aria-label={showPassword ? 'Hide password' : 'Show password'}
                  >
                    {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                  </button>
                </div>
                {errors.password && <p className="mt-1.5 text-xs font-medium text-rose-600">{errors.password.message}</p>}
              </div>

              {serverError && (
                <div className="rounded-xl border border-rose-100 bg-rose-50 px-3.5 py-3 text-sm text-rose-700" role="alert">
                  {serverError}
                </div>
              )}

              <button
                type="submit"
                disabled={isSubmitting}
                className="flex w-full items-center justify-center gap-2 rounded-xl bg-sky-600 px-4 py-3 text-sm font-bold text-white shadow-lg shadow-sky-600/20 transition hover:bg-sky-700 disabled:cursor-not-allowed disabled:opacity-60"
              >
                {isSubmitting ? 'Signing in…' : 'Sign in'}
                {!isSubmitting && <ArrowRight className="h-4 w-4" />}
              </button>
            </form>
            <p className="mt-6 text-center text-sm text-slate-500">
              Don&apos;t have an account?{' '}
              <Link to="/signup" className="font-bold text-sky-700 hover:text-sky-800">
                Create one
              </Link>
            </p>
          </div>
          <p className="mt-6 text-center text-xs text-slate-400">
            Protected clinical environment · Authorized staff only
          </p>
        </div>
      </section>
    </div>
  )
}
