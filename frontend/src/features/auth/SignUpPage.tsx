import { zodResolver } from '@hookform/resolvers/zod'
import { Activity, ArrowLeft, ArrowRight, CheckCircle2, Eye, EyeOff, ShieldCheck, UserPlus } from 'lucide-react'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { z } from 'zod'
import { ApiRequestError } from '../../api/httpClient'
import { useAuth } from '../../auth/AuthProvider'

const signUpSchema = z.object({
  email: z.string().trim().min(1, 'Email is required').email('Enter a valid email'),
  password: z.string().min(8, 'Password must contain at least 8 characters').max(72, 'Password cannot exceed 72 characters'),
  confirmPassword: z.string().min(1, 'Please confirm your password'),
}).refine((values) => values.password === values.confirmPassword, {
  message: 'Passwords do not match',
  path: ['confirmPassword'],
})

type SignUpForm = z.infer<typeof signUpSchema>
const inputClass = 'mt-2 w-full rounded-xl border border-slate-200 bg-white px-3.5 py-3 text-sm outline-none transition placeholder:text-slate-400 focus:border-sky-500 focus:ring-4 focus:ring-sky-100'

export function SignUpPage() {
  const [showPassword, setShowPassword] = useState(false)
  const [serverError, setServerError] = useState<string | null>(null)
  const { user, registerAccount } = useAuth()
  const navigate = useNavigate()
  const { register, handleSubmit, setError, formState: { errors, isSubmitting } } = useForm<SignUpForm>({ resolver: zodResolver(signUpSchema) })

  if (user) return <Navigate to="/dashboard" replace />

  const onSubmit = async ({ email, password }: SignUpForm) => {
    setServerError(null)
    try {
      await registerAccount({ email, password })
      navigate('/dashboard', { replace: true })
    } catch (error) {
      if (error instanceof ApiRequestError) {
        Object.entries(error.fieldErrors).forEach(([field, message]) => {
          if (field === 'email' || field === 'password') setError(field, { message })
        })
        setServerError(error.message)
      } else {
        setServerError('We could not create your account. Please try again.')
      }
    }
  }

  return (
    <div className="grid min-h-screen bg-white lg:grid-cols-[0.9fr_1.1fr]">
      <section className="relative hidden overflow-hidden bg-slate-950 p-12 text-white lg:flex lg:flex-col lg:justify-between">
        <div className="absolute -left-36 top-12 h-96 w-96 rounded-full bg-sky-500/20 blur-3xl" />
        <div className="absolute -bottom-24 right-0 h-96 w-96 rounded-full bg-cyan-400/10 blur-3xl" />
        <Link to="/" className="relative flex items-center gap-3">
          <div className="grid h-11 w-11 place-items-center rounded-2xl bg-white/10 ring-1 ring-white/20"><Activity className="h-6 w-6" /></div>
          <div><div className="text-lg font-bold">HealthFlow</div><div className="text-xs font-medium text-slate-300">Clinical operations, connected.</div></div>
        </Link>

        <div className="relative max-w-lg">
          <div className="mb-7 inline-flex items-center gap-2 rounded-full border border-white/15 bg-white/5 px-3 py-1.5 text-xs font-semibold text-sky-200"><ShieldCheck className="h-4 w-4" /> Secure account creation</div>
          <h1 className="text-5xl font-bold leading-[1.08] tracking-tight">Join a clearer way to coordinate care.</h1>
          <p className="mt-6 text-base leading-7 text-slate-300">Create your HealthFlow account and start working from one focused, role-aware clinical workspace.</p>
          <div className="mt-8 space-y-3 text-sm text-slate-200">
            {['Secure password storage', 'Short-lived authenticated sessions', 'Backend-enforced permissions'].map((item) => (
              <div key={item} className="flex items-center gap-3"><CheckCircle2 className="h-4 w-4 text-emerald-400" /> {item}</div>
            ))}
          </div>
        </div>

        <p className="relative text-xs leading-5 text-slate-400">New self-service accounts receive Receptionist permissions. An administrator can manage roles later.</p>
      </section>

      <section className="flex items-center justify-center bg-slate-50/60 px-5 py-10 sm:px-10">
        <div className="w-full max-w-md">
          <Link to="/" className="mb-8 inline-flex items-center gap-2 text-sm font-semibold text-slate-500 hover:text-slate-900"><ArrowLeft className="h-4 w-4" /> Back to home</Link>
          <div className="rounded-3xl border border-slate-200/80 bg-white p-6 shadow-xl shadow-slate-900/[0.04] sm:p-9">
            <div className="grid h-12 w-12 place-items-center rounded-2xl bg-sky-50 text-sky-700"><UserPlus className="h-6 w-6" /></div>
            <h2 className="mt-6 text-3xl font-bold tracking-tight text-slate-950">Create your account</h2>
            <p className="mt-2 text-sm leading-6 text-slate-500">Enter your details to begin using HealthFlow.</p>

            <form onSubmit={handleSubmit(onSubmit)} className="mt-8 space-y-5" noValidate>
              <div>
                <label htmlFor="signup-email" className="text-sm font-semibold text-slate-700">Email address</label>
                <input id="signup-email" type="email" autoComplete="email" placeholder="you@healthflow.com" className={inputClass} {...register('email')} />
                {errors.email && <p className="mt-1.5 text-xs font-medium text-rose-600">{errors.email.message}</p>}
              </div>
              <div>
                <label htmlFor="signup-password" className="text-sm font-semibold text-slate-700">Password</label>
                <div className="relative">
                  <input id="signup-password" type={showPassword ? 'text' : 'password'} autoComplete="new-password" placeholder="At least 8 characters" className={`${inputClass} pr-11`} {...register('password')} />
                  <button type="button" onClick={() => setShowPassword((visible) => !visible)} className="absolute bottom-0 right-0 grid h-11 w-11 place-items-center text-slate-400 hover:text-slate-700" aria-label={showPassword ? 'Hide passwords' : 'Show passwords'}>
                    {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                  </button>
                </div>
                {errors.password && <p className="mt-1.5 text-xs font-medium text-rose-600">{errors.password.message}</p>}
              </div>
              <div>
                <label htmlFor="confirm-password" className="text-sm font-semibold text-slate-700">Confirm password</label>
                <input id="confirm-password" type={showPassword ? 'text' : 'password'} autoComplete="new-password" placeholder="Enter the password again" className={inputClass} {...register('confirmPassword')} />
                {errors.confirmPassword && <p className="mt-1.5 text-xs font-medium text-rose-600">{errors.confirmPassword.message}</p>}
              </div>

              {serverError && <div className="rounded-xl border border-rose-100 bg-rose-50 px-3.5 py-3 text-sm text-rose-700" role="alert">{serverError}</div>}
              <button type="submit" disabled={isSubmitting} className="flex w-full items-center justify-center gap-2 rounded-xl bg-sky-600 px-4 py-3 text-sm font-bold text-white shadow-lg shadow-sky-600/20 transition hover:bg-sky-700 disabled:cursor-not-allowed disabled:opacity-60">
                {isSubmitting ? 'Creating account…' : 'Create account'} {!isSubmitting && <ArrowRight className="h-4 w-4" />}
              </button>
            </form>

            <p className="mt-6 text-center text-sm text-slate-500">Already have an account?{' '}<Link to="/login" className="font-bold text-sky-700 hover:text-sky-800">Sign in</Link></p>
          </div>
        </div>
      </section>
    </div>
  )
}
