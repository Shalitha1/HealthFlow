import {
  Activity,
  ArrowRight,
  CalendarCheck,
  CheckCircle2,
  ClipboardCheck,
  HeartPulse,
  Menu,
  ShieldCheck,
  Sparkles,
  Users,
  X,
} from 'lucide-react'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../auth/AuthProvider'

const features = [
  {
    icon: Users,
    title: 'Patient records with context',
    description:
      'Find, review, and update patient information from one calm operational workspace.',
  },
  {
    icon: ClipboardCheck,
    title: 'Clear daily workflows',
    description:
      'Move from registration to follow-up with focused views and role-aware navigation.',
  },
  {
    icon: ShieldCheck,
    title: 'Secure by design',
    description:
      'Short-lived sessions and backend role enforcement help protect every clinical action.',
  },
]

export function LandingPage() {
  const [mobileOpen, setMobileOpen] = useState(false)
  const { user } = useAuth()
  const workspacePath = user ? '/dashboard' : '/login'

  return (
    <div className="min-h-screen overflow-hidden bg-white text-slate-950">
      <header className="relative z-30 border-b border-slate-200/70 bg-white/90 backdrop-blur-xl">
        <div className="mx-auto flex h-18 max-w-7xl items-center justify-between px-5 sm:px-8">
          <Link to="/" className="flex items-center gap-3" aria-label="HealthFlow home">
            <div className="grid h-10 w-10 place-items-center rounded-2xl bg-sky-600 text-white shadow-lg shadow-sky-600/20">
              <Activity className="h-5 w-5" strokeWidth={2.5} />
            </div>
            <div>
              <div className="font-bold tracking-tight">HealthFlow</div>
              <div className="text-[10px] font-bold uppercase tracking-[0.18em] text-slate-400">
                Clinical workspace
              </div>
            </div>
          </Link>

          <nav className="hidden items-center gap-8 text-sm font-semibold text-slate-600 md:flex">
            <a href="#platform" className="hover:text-sky-700">Platform</a>
            <a href="#workflow" className="hover:text-sky-700">How it works</a>
            <a href="#security" className="hover:text-sky-700">Security</a>
          </nav>

          <div className="hidden items-center gap-3 md:flex">
            {!user && (
              <Link to="/login" className="rounded-xl px-4 py-2.5 text-sm font-bold text-slate-700 hover:bg-slate-50">
                Sign in
              </Link>
            )}
            <Link to={user ? workspacePath : '/signup'} className="inline-flex items-center gap-2 rounded-xl bg-sky-600 px-4 py-2.5 text-sm font-bold text-white shadow-lg shadow-sky-600/15 hover:bg-sky-700">
              {user ? 'Open dashboard' : 'Create account'}
              <ArrowRight className="h-4 w-4" />
            </Link>
          </div>

          <button type="button" onClick={() => setMobileOpen((open) => !open)} className="rounded-xl p-2 text-slate-600 hover:bg-slate-100 md:hidden" aria-label="Toggle navigation">
            {mobileOpen ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
          </button>
        </div>

        {mobileOpen && (
          <div className="border-t border-slate-100 bg-white px-5 py-4 md:hidden">
            <nav className="space-y-1 text-sm font-semibold text-slate-700">
              <a href="#platform" onClick={() => setMobileOpen(false)} className="block rounded-xl px-3 py-2.5 hover:bg-slate-50">Platform</a>
              <a href="#workflow" onClick={() => setMobileOpen(false)} className="block rounded-xl px-3 py-2.5 hover:bg-slate-50">How it works</a>
              <a href="#security" onClick={() => setMobileOpen(false)} className="block rounded-xl px-3 py-2.5 hover:bg-slate-50">Security</a>
              {!user && <Link to="/login" className="block rounded-xl px-3 py-2.5 text-center hover:bg-slate-50">Sign in</Link>}
              <Link to={user ? workspacePath : '/signup'} className="mt-3 flex items-center justify-center gap-2 rounded-xl bg-sky-600 px-4 py-3 font-bold text-white">
                {user ? 'Open dashboard' : 'Create account'} <ArrowRight className="h-4 w-4" />
              </Link>
            </nav>
          </div>
        )}
      </header>

      <main>
        <section className="relative isolate">
          <div className="absolute inset-0 -z-10 bg-[radial-gradient(circle_at_80%_15%,rgba(125,211,252,0.22),transparent_30%),radial-gradient(circle_at_10%_70%,rgba(186,230,253,0.35),transparent_28%)]" />
          <div className="mx-auto grid max-w-7xl gap-14 px-5 py-18 sm:px-8 sm:py-24 lg:grid-cols-[1.02fr_0.98fr] lg:items-center lg:py-30">
            <div>
              <div className="inline-flex items-center gap-2 rounded-full border border-sky-200 bg-sky-50 px-3 py-1.5 text-xs font-bold text-sky-700">
                <Sparkles className="h-3.5 w-3.5" /> Built for focused care operations
              </div>
              <h1 className="mt-6 max-w-3xl text-4xl font-bold leading-[1.08] tracking-[-0.04em] text-slate-950 sm:text-6xl">
                Keep every patient journey moving with clarity.
              </h1>
              <p className="mt-6 max-w-xl text-base leading-8 text-slate-600 sm:text-lg">
                HealthFlow brings patient records, operational insight, and care-team activity into one secure, thoughtfully organized workspace.
              </p>
              <div className="mt-8 flex flex-col gap-3 sm:flex-row">
                <Link to={user ? workspacePath : '/signup'} className="inline-flex items-center justify-center gap-2 rounded-xl bg-sky-600 px-5 py-3.5 text-sm font-bold text-white shadow-xl shadow-sky-600/20 hover:bg-sky-700">
                  {user ? 'Continue to dashboard' : 'Create your account'}
                  <ArrowRight className="h-4 w-4" />
                </Link>
                <a href="#platform" className="inline-flex items-center justify-center rounded-xl border border-slate-200 bg-white px-5 py-3.5 text-sm font-bold text-slate-700 hover:bg-slate-50">
                  Explore the platform
                </a>
              </div>
              <div className="mt-8 flex flex-wrap gap-x-6 gap-y-3 text-xs font-semibold text-slate-500">
                {['Role-aware access', 'Patient history retained', 'Live service health'].map((item) => (
                  <span key={item} className="flex items-center gap-2">
                    <CheckCircle2 className="h-4 w-4 text-emerald-500" /> {item}
                  </span>
                ))}
              </div>
            </div>

            <div className="relative mx-auto w-full max-w-xl">
              <div className="absolute -inset-8 -z-10 rounded-[3rem] bg-sky-100/60 blur-3xl" />
              <div className="rounded-[2rem] border border-slate-200/80 bg-white p-4 shadow-2xl shadow-slate-900/10 sm:p-6">
                <div className="flex items-center justify-between border-b border-slate-100 pb-5">
                  <div>
                    <p className="text-xs font-bold uppercase tracking-[0.18em] text-sky-600">Today at a glance</p>
                    <h2 className="mt-1 text-lg font-bold">Clinical operations</h2>
                  </div>
                  <div className="flex items-center gap-2 rounded-full bg-emerald-50 px-3 py-1.5 text-xs font-bold text-emerald-700">
                    <span className="h-2 w-2 rounded-full bg-emerald-500" /> Systems ready
                  </div>
                </div>

                <div className="mt-5 grid grid-cols-2 gap-3">
                  <div className="rounded-2xl bg-sky-50 p-4">
                    <Users className="h-5 w-5 text-sky-700" />
                    <div className="mt-5 text-2xl font-bold">Patient-first</div>
                    <p className="mt-1 text-xs leading-5 text-sky-800/70">Records stay clear, searchable, and connected.</p>
                  </div>
                  <div className="rounded-2xl bg-violet-50 p-4">
                    <CalendarCheck className="h-5 w-5 text-violet-700" />
                    <div className="mt-5 text-2xl font-bold">One flow</div>
                    <p className="mt-1 text-xs leading-5 text-violet-800/70">A workspace ready for each care milestone.</p>
                  </div>
                </div>

                <div className="mt-4 rounded-2xl border border-slate-200 p-4">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-3">
                      <div className="grid h-10 w-10 place-items-center rounded-xl bg-emerald-50 text-emerald-700"><HeartPulse className="h-5 w-5" /></div>
                      <div><p className="text-sm font-bold">Care coordination</p><p className="mt-0.5 text-xs text-slate-500">The right context, when it matters</p></div>
                    </div>
                    <ArrowRight className="h-4 w-4 text-slate-300" />
                  </div>
                </div>
              </div>
            </div>
          </div>
        </section>

        <section id="platform" className="border-y border-slate-200/70 bg-slate-50/70 py-20 sm:py-24">
          <div className="mx-auto max-w-7xl px-5 sm:px-8">
            <div className="max-w-2xl">
              <p className="text-xs font-bold uppercase tracking-[0.2em] text-sky-600">A better operating rhythm</p>
              <h2 className="mt-3 text-3xl font-bold tracking-tight sm:text-4xl">Everything your team needs to stay oriented.</h2>
              <p className="mt-4 text-sm leading-7 text-slate-600">Designed around focused tasks instead of crowded screens, HealthFlow keeps essential actions easy to find.</p>
            </div>
            <div className="mt-10 grid gap-5 md:grid-cols-3">
              {features.map((feature) => {
                const Icon = feature.icon
                return (
                  <article key={feature.title} className="rounded-2xl border border-slate-200/80 bg-white p-6 shadow-sm shadow-slate-900/[0.02]">
                    <div className="grid h-11 w-11 place-items-center rounded-xl bg-sky-50 text-sky-700"><Icon className="h-5 w-5" /></div>
                    <h3 className="mt-5 text-lg font-bold">{feature.title}</h3>
                    <p className="mt-2 text-sm leading-6 text-slate-500">{feature.description}</p>
                  </article>
                )
              })}
            </div>
          </div>
        </section>

        <section id="workflow" className="py-20 sm:py-24">
          <div className="mx-auto max-w-7xl px-5 sm:px-8">
            <div className="grid gap-12 lg:grid-cols-[0.8fr_1.2fr] lg:items-start">
              <div className="lg:sticky lg:top-24">
                <p className="text-xs font-bold uppercase tracking-[0.2em] text-sky-600">How it works</p>
                <h2 className="mt-3 text-3xl font-bold tracking-tight sm:text-4xl">From sign-in to patient insight in moments.</h2>
                <p className="mt-4 text-sm leading-7 text-slate-600">A simple, secure flow helps each role focus on the work it is authorized to perform.</p>
              </div>
              <div className="space-y-4">
                {[
                  ['01', 'Sign in securely', 'Your short-lived session is validated before protected information is shown.'],
                  ['02', 'See the right workspace', 'Navigation and actions adapt to administrator, receptionist, and doctor roles.'],
                  ['03', 'Move care forward', 'Search records, update patient details, and keep historical status visible.'],
                ].map(([number, title, description]) => (
                  <div key={number} className="flex gap-5 rounded-2xl border border-slate-200 bg-white p-5 sm:p-6">
                    <div className="text-sm font-bold text-sky-600">{number}</div>
                    <div><h3 className="font-bold">{title}</h3><p className="mt-2 text-sm leading-6 text-slate-500">{description}</p></div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </section>

        <section id="security" className="px-5 pb-20 sm:px-8 sm:pb-24">
          <div className="mx-auto max-w-7xl overflow-hidden rounded-[2rem] bg-slate-950 px-6 py-12 text-white sm:px-10 lg:flex lg:items-center lg:justify-between lg:px-14">
            <div className="max-w-2xl">
              <div className="flex items-center gap-2 text-xs font-bold uppercase tracking-[0.2em] text-sky-300"><ShieldCheck className="h-4 w-4" /> Security built into the workflow</div>
              <h2 className="mt-4 text-3xl font-bold tracking-tight">Ready to enter your clinical workspace?</h2>
              <p className="mt-3 text-sm leading-7 text-slate-300">Authorized staff can sign in to access protected, role-aware HealthFlow features.</p>
            </div>
            <Link to={user ? workspacePath : '/signup'} className="mt-7 inline-flex shrink-0 items-center gap-2 rounded-xl bg-white px-5 py-3.5 text-sm font-bold text-slate-950 hover:bg-sky-50 lg:mt-0">
              {user ? 'Open dashboard' : 'Create account'} <ArrowRight className="h-4 w-4" />
            </Link>
          </div>
        </section>
      </main>

      <footer className="border-t border-slate-200 bg-slate-50/60">
        <div className="mx-auto flex max-w-7xl flex-col gap-3 px-5 py-7 text-xs text-slate-500 sm:flex-row sm:items-center sm:justify-between sm:px-8">
          <div className="flex items-center gap-2 font-semibold text-slate-700"><Activity className="h-4 w-4 text-sky-600" /> HealthFlow</div>
          <p>Learning-focused clinical operations platform.</p>
        </div>
      </footer>
    </div>
  )
}
