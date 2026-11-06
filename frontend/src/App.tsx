import type { ReactNode } from 'react'
import { Navigate, Route, Routes, useLocation } from 'react-router'
import { useAuth } from './auth'
import Layout, { NAV_ITEMS } from './layout/Layout'
import DashboardPage from './pages/DashboardPage'
import LoginPage from './pages/LoginPage'
import PlaceholderPage from './pages/PlaceholderPage'
import UsersPage from './pages/UsersPage'

// Pages that are already built. Everything else in the sidebar shows a placeholder.
const BUILT: Record<string, ReactNode> = {
  '/dashboard': <DashboardPage />,
  '/users': <UsersPage />,
}

function RequireLogin({ children }: { children: ReactNode }) {
  const { user, loading } = useAuth()
  const location = useLocation()
  if (loading) return <p className="muted center">Loading...</p>
  if (!user) return <Navigate to="/login" replace state={{ from: location.pathname }} />
  return children
}

function RequireAdmin({ children }: { children: ReactNode }) {
  const { isAdmin } = useAuth()
  if (!isAdmin) return <PlaceholderPage title="Not allowed" message="Only admins can open this page." />
  return children
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route
        element={
          <RequireLogin>
            <Layout />
          </RequireLogin>
        }
      >
        <Route index element={<Navigate to="/dashboard" replace />} />
        {NAV_ITEMS.map((item) => {
          let page = BUILT[item.path] ?? <PlaceholderPage title={item.label} phase={item.phase} />
          if (item.adminOnly) page = <RequireAdmin>{page}</RequireAdmin>
          return <Route key={item.path} path={item.path.slice(1)} element={page} />
        })}
        <Route path="*" element={<PlaceholderPage title="Page not found" />} />
      </Route>
    </Routes>
  )
}
