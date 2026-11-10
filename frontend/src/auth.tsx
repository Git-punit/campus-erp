import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from 'react'
import { api, getToken, setToken, setUnauthorizedHandler } from './api'

export type Role = 'ADMIN' | 'STAFF' | 'VIEWER'

export interface User {
  id: number
  username: string
  fullName: string
  role: Role
  enabled: boolean
  createdAt: string
  email: string | null
  phone: string | null
  designation: string | null
  hasPhoto: boolean
}

interface AuthState {
  user: User | null
  loading: boolean // true while checking a saved token on page load
  login: (username: string, password: string) => Promise<void>
  logout: () => void
  setUser: (user: User) => void // after editing your own account
  isAdmin: boolean
  canEdit: boolean // ADMIN or STAFF
}

const AuthContext = createContext<AuthState | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(() => getToken() !== null)

  const logout = useCallback(() => {
    setToken(null)
    setUser(null)
  }, [])

  useEffect(() => {
    setUnauthorizedHandler(() => setUser(null))
    return () => setUnauthorizedHandler(null)
  }, [])

  // If there's a token from last time, find out who it belongs to
  useEffect(() => {
    if (!getToken()) return
    api
      .get<User>('/auth/me')
      .then(setUser)
      .catch(() => setToken(null))
      .finally(() => setLoading(false))
  }, [])

  const login = useCallback(async (username: string, password: string) => {
    const res = await api.post<{ token: string; user: User }>('/auth/login', { username, password })
    setToken(res.token)
    setUser(res.user)
  }, [])

  const value: AuthState = {
    user,
    loading,
    login,
    logout,
    setUser,
    isAdmin: user?.role === 'ADMIN',
    canEdit: user?.role === 'ADMIN' || user?.role === 'STAFF',
  }
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider')
  return ctx
}
