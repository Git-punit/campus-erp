import { NavLink, Outlet } from 'react-router'
import { useAuth } from '../auth'

export interface NavItem {
  path: string
  label: string
  phase: number // build plan phase that fills this page in
  adminOnly?: boolean
}

export const NAV_ITEMS: NavItem[] = [
  { path: '/dashboard', label: 'Dashboard', phase: 10 },
  { path: '/faculty', label: 'Faculty', phase: 3 },
  { path: '/courses', label: 'Courses', phase: 3 },
  { path: '/students', label: 'Students', phase: 4 },
  { path: '/calendar', label: 'Calendar', phase: 5 },
  { path: '/budgets', label: 'Budgets', phase: 6 },
  { path: '/projects', label: 'Research projects', phase: 7 },
  { path: '/training', label: 'Training programs', phase: 8 },
  { path: '/events', label: 'Events', phase: 9 },
  { path: '/setup', label: 'Academic setup', phase: 2 },
  { path: '/users', label: 'Users', phase: 1, adminOnly: true },
]

export default function Layout() {
  const { user, isAdmin, logout } = useAuth()

  return (
    <div className="app">
      <aside className="sidebar">
        <div className="brand">Campus ERP</div>
        <nav>
          {NAV_ITEMS.filter((item) => !item.adminOnly || isAdmin).map((item) => (
            <NavLink key={item.path} to={item.path}>
              {item.label}
            </NavLink>
          ))}
        </nav>
        {user && (
          <div className="sidebar-user">
            <div>{user.fullName}</div>
            <div className="role">{user.role.toLowerCase()}</div>
            <button className="link-btn" onClick={logout}>
              Log out
            </button>
          </div>
        )}
      </aside>
      <main className="content">
        <Outlet />
      </main>
    </div>
  )
}
