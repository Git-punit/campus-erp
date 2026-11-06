import { NavLink, Outlet } from 'react-router'
import { useAuth } from '../auth'

export interface NavItem {
  path: string
  label: string
  adminOnly?: boolean
}

export const NAV_ITEMS: NavItem[] = [
  { path: '/dashboard', label: 'Dashboard' },
  { path: '/faculty', label: 'Faculty' },
  { path: '/courses', label: 'Courses' },
  { path: '/students', label: 'Students' },
  { path: '/calendar', label: 'Calendar' },
  { path: '/budgets', label: 'Budgets' },
  { path: '/projects', label: 'Research projects' },
  { path: '/training', label: 'Training programs' },
  { path: '/events', label: 'Events' },
  { path: '/setup', label: 'Academic setup' },
  { path: '/users', label: 'Users', adminOnly: true },
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
