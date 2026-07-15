import { Navigate, Outlet, useLocation } from 'react-router-dom'
import type { UserRole } from '../types/api'
import { LoadingState } from '../components/feedback/LoadingState'
import { useAuth } from './AuthProvider'

export function ProtectedRoute({ allowedRoles }: { allowedRoles?: UserRole[] }) {
  const { user, isInitializing } = useAuth()
  const location = useLocation()

  if (isInitializing) {
    return <LoadingState fullScreen label="Restoring your secure session" />
  }

  if (!user) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }

  if (allowedRoles && !allowedRoles.includes(user.role)) {
    return <Navigate to="/forbidden" replace />
  }

  return <Outlet />
}
