// oxlint-disable react/only-export-components -- provider and hook share one context module
import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import { authApi } from '../api/authApi'
import {
  clearAccessToken,
  getAccessToken,
  setAccessToken,
} from '../api/httpClient'
import type { CurrentUser, LoginRequest, RegisterRequest } from '../types/api'

interface AuthContextValue {
  user: CurrentUser | null
  isInitializing: boolean
  login: (request: LoginRequest) => Promise<CurrentUser>
  registerAccount: (request: RegisterRequest) => Promise<CurrentUser>
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<CurrentUser | null>(null)
  const [isInitializing, setIsInitializing] = useState(Boolean(getAccessToken()))

  const logout = useCallback(() => {
    clearAccessToken()
    setUser(null)
  }, [])

  useEffect(() => {
    const handleUnauthorized = () => logout()
    window.addEventListener('healthflow:unauthorized', handleUnauthorized)
    return () =>
      window.removeEventListener('healthflow:unauthorized', handleUnauthorized)
  }, [logout])

  useEffect(() => {
    if (!getAccessToken()) {
      setIsInitializing(false)
      return
    }

    authApi
      .me()
      .then(setUser)
      .catch(logout)
      .finally(() => setIsInitializing(false))
  }, [logout])

  const login = useCallback(async (request: LoginRequest) => {
    const response = await authApi.login(request)
    setAccessToken(response.token)
    try {
      const currentUser = await authApi.me()
      setUser(currentUser)
      return currentUser
    } catch (error) {
      clearAccessToken()
      throw error
    }
  }, [])

  const registerAccount = useCallback(async (request: RegisterRequest) => {
    const response = await authApi.register(request)
    setAccessToken(response.token)
    try {
      const currentUser = await authApi.me()
      setUser(currentUser)
      return currentUser
    } catch (error) {
      clearAccessToken()
      throw error
    }
  }, [])

  const value = useMemo(
    () => ({ user, isInitializing, login, registerAccount, logout }),
    [isInitializing, login, logout, registerAccount, user],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth must be used inside AuthProvider')
  return context
}
