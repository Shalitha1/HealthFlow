import { Route, Routes } from 'react-router-dom'
import { ProtectedRoute } from './auth/ProtectedRoute'
import { AppLayout } from './components/layout/AppLayout'
import { AuditPage } from './features/audit/AuditPage'
import { LoginPage } from './features/auth/LoginPage'
import { SignUpPage } from './features/auth/SignUpPage'
import { DashboardPage } from './features/dashboard/DashboardPage'
import { PatientDetailsPage } from './features/patients/PatientDetailsPage'
import { PatientFormPage } from './features/patients/PatientFormPage'
import { PatientListPage } from './features/patients/PatientListPage'
import { ComingSoonPage } from './pages/ComingSoonPage'
import { ForbiddenPage } from './pages/ForbiddenPage'
import { LandingPage } from './pages/LandingPage'
import { NotFoundPage } from './pages/NotFoundPage'

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<LandingPage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/signup" element={<SignUpPage />} />

      <Route element={<ProtectedRoute />}>
        <Route element={<AppLayout />}>
          <Route path="dashboard" element={<DashboardPage />} />
          <Route path="forbidden" element={<ForbiddenPage />} />
          <Route path="appointments" element={<ComingSoonPage title="Appointments" description="Scheduling, calendars, and care-team coordination will live here." />} />
          <Route path="notifications" element={<ComingSoonPage title="Notifications" description="Delivery history and communication preferences will live here." />} />

          <Route element={<ProtectedRoute allowedRoles={['ADMIN', 'RECEPTIONIST']} />}>
            <Route path="patients" element={<PatientListPage />} />
            <Route path="patients/new" element={<PatientFormPage />} />
            <Route path="patients/:id/edit" element={<PatientFormPage />} />
            <Route path="billing" element={<ComingSoonPage title="Billing" description="Accounts, claims, balances, and payment workflows will live here." />} />
          </Route>

          <Route path="patients/:id" element={<PatientDetailsPage />} />

          <Route element={<ProtectedRoute allowedRoles={['ADMIN']} />}>
            <Route path="audit" element={<AuditPage />} />
          </Route>
        </Route>
      </Route>

      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  )
}
