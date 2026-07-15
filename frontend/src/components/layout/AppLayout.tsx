import {
  Activity,
  Bell,
  CalendarDays,
  ChevronDown,
  ClipboardList,
  LayoutDashboard,
  LogOut,
  Menu,
  PanelLeftClose,
  ReceiptText,
  ShieldCheck,
  Users,
  X,
  type LucideIcon,
} from 'lucide-react'
import { useMemo, useState } from 'react'
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../../auth/AuthProvider'
import type { UserRole } from '../../types/api'

interface NavigationItem {
  label: string
  to: string
  icon: LucideIcon
  roles?: UserRole[]
  disabled?: boolean
}

const navigation: NavigationItem[] = [
  { label: 'Dashboard', to: '/dashboard', icon: LayoutDashboard },
  {
    label: 'Patients',
    to: '/patients',
    icon: Users,
    roles: ['ADMIN', 'RECEPTIONIST'],
  },
  { label: 'Appointments', to: '/appointments', icon: CalendarDays },
  {
    label: 'Billing',
    to: '/billing',
    icon: ReceiptText,
    roles: ['ADMIN', 'RECEPTIONIST'],
  },
  { label: 'Notifications', to: '/notifications', icon: Bell },
  { label: 'Audit trail', to: '/audit', icon: ClipboardList, roles: ['ADMIN'] },
]

function Brand() {
  return (
    <div className="flex items-center gap-3">
      <div className="grid h-10 w-10 place-items-center rounded-2xl bg-sky-600 text-white shadow-lg shadow-sky-600/20">
        <Activity className="h-5 w-5" strokeWidth={2.5} />
      </div>
      <div>
        <div className="font-bold tracking-tight text-slate-950">HealthFlow</div>
        <div className="text-[11px] font-semibold uppercase tracking-[0.18em] text-slate-400">
          Clinical workspace
        </div>
      </div>
    </div>
  )
}

function Navigation({ close }: { close?: () => void }) {
  const { user } = useAuth()
  const visibleItems = navigation.filter(
    (item) => !item.roles || (user && item.roles.includes(user.role)),
  )

  return (
    <nav className="mt-8 space-y-1.5" aria-label="Primary navigation">
      {visibleItems.map((item) => {
        const Icon = item.icon
        return (
          <NavLink
            key={item.to}
            to={item.to}
            end={item.to === '/dashboard'}
            onClick={close}
            className={({ isActive }) =>
              `group flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-semibold transition ${
                isActive
                  ? 'bg-sky-50 text-sky-700'
                  : 'text-slate-600 hover:bg-slate-50 hover:text-slate-950'
              }`
            }
          >
            <Icon className="h-[18px] w-[18px]" />
            {item.label}
          </NavLink>
        )
      })}
    </nav>
  )
}

export function AppLayout() {
  const [mobileOpen, setMobileOpen] = useState(false)
  const [userMenuOpen, setUserMenuOpen] = useState(false)
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  const currentTitle = useMemo(() => {
    const item = [...navigation]
      .sort((a, b) => b.to.length - a.to.length)
      .find((entry) =>
        location.pathname.startsWith(entry.to),
      )
    if (location.pathname === '/appointments/new') return 'New appointment'
    if (location.pathname === '/appointments/my-schedule') return 'Daily schedule'
    if (location.pathname.startsWith('/appointments/') && location.pathname.endsWith('/edit')) return 'Reschedule appointment'
    if (location.pathname.startsWith('/appointments/')) return 'Appointment details'
    if (location.pathname === '/patients/new') return 'New patient'
    if (location.pathname === '/billing/invoices/new') return 'Create invoice'
    if (location.pathname.endsWith('/payment') && location.pathname.startsWith('/billing/invoices/')) return 'Record payment'
    if (location.pathname.startsWith('/billing/accounts/patient/')) return 'Patient billing'
    if (location.pathname.startsWith('/billing/invoices/')) return 'Invoice details'
    if (location.pathname === '/billing/invoices') return 'Invoices'
    if (location.pathname.startsWith('/patients/') && location.pathname.endsWith('/edit')) return 'Edit patient'
    return item?.label ?? 'Patient details'
  }, [location.pathname])

  const handleLogout = () => {
    logout()
    navigate('/', { replace: true })
  }

  return (
    <div className="min-h-screen bg-slate-50/80 text-slate-900">
      <aside className="fixed inset-y-0 left-0 z-30 hidden w-64 border-r border-slate-200/80 bg-white px-5 py-6 lg:block">
        <Brand />
        <Navigation />
        <div className="absolute inset-x-5 bottom-6 rounded-2xl bg-slate-50 p-4">
          <div className="flex items-center gap-2 text-xs font-semibold text-emerald-700">
            <ShieldCheck className="h-4 w-4" /> Secure session
          </div>
          <p className="mt-1.5 text-xs leading-5 text-slate-500">
            Access is protected by short-lived credentials.
          </p>
        </div>
      </aside>

      {mobileOpen && (
        <div className="fixed inset-0 z-50 lg:hidden">
          <button
            className="absolute inset-0 bg-slate-950/30 backdrop-blur-sm"
            aria-label="Close navigation"
            onClick={() => setMobileOpen(false)}
          />
          <aside className="relative h-full w-[min(85vw,20rem)] bg-white px-5 py-5 shadow-2xl">
            <div className="flex items-center justify-between">
              <Brand />
              <button
                className="rounded-xl p-2 text-slate-500 hover:bg-slate-100"
                onClick={() => setMobileOpen(false)}
                aria-label="Close menu"
              >
                <X className="h-5 w-5" />
              </button>
            </div>
            <Navigation close={() => setMobileOpen(false)} />
          </aside>
        </div>
      )}

      <div className="lg:pl-64">
        <header className="sticky top-0 z-20 flex h-16 items-center justify-between border-b border-slate-200/80 bg-white/90 px-4 backdrop-blur-xl sm:px-6 lg:px-8">
          <div className="flex items-center gap-3">
            <button
              type="button"
              className="rounded-xl p-2 text-slate-600 hover:bg-slate-100 lg:hidden"
              onClick={() => setMobileOpen(true)}
              aria-label="Open menu"
            >
              <Menu className="h-5 w-5" />
            </button>
            <PanelLeftClose className="hidden h-4 w-4 text-slate-300 lg:block" />
            <span className="text-sm font-semibold text-slate-700">{currentTitle}</span>
          </div>

          <div className="relative">
            <button
              type="button"
              onClick={() => setUserMenuOpen((open) => !open)}
              className="flex items-center gap-3 rounded-xl p-1.5 pl-2 hover:bg-slate-50"
              aria-expanded={userMenuOpen}
            >
              <div className="hidden text-right sm:block">
                <div className="max-w-44 truncate text-xs font-semibold text-slate-800">
                  {user?.email}
                </div>
                <div className="text-[10px] font-bold uppercase tracking-wider text-sky-600">
                  {user?.role}
                </div>
              </div>
              <div className="grid h-9 w-9 place-items-center rounded-xl bg-sky-100 text-sm font-bold text-sky-700">
                {user?.email.charAt(0).toUpperCase()}
              </div>
              <ChevronDown className="h-4 w-4 text-slate-400" />
            </button>

            {userMenuOpen && (
              <div className="absolute right-0 mt-2 w-64 rounded-2xl border border-slate-200 bg-white p-2 shadow-xl shadow-slate-900/10">
                <div className="border-b border-slate-100 px-3 py-2.5 sm:hidden">
                  <p className="truncate text-sm font-semibold">{user?.email}</p>
                  <p className="mt-0.5 text-xs text-slate-500">{user?.role}</p>
                </div>
                <button
                  type="button"
                  onClick={handleLogout}
                  className="flex w-full items-center gap-2 rounded-xl px-3 py-2.5 text-sm font-semibold text-rose-600 hover:bg-rose-50"
                >
                  <LogOut className="h-4 w-4" /> Log out
                </button>
              </div>
            )}
          </div>
        </header>

        <main className="mx-auto max-w-[1500px] px-4 py-6 sm:px-6 lg:px-8 lg:py-8">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
