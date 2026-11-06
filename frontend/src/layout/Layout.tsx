import { NavLink, Outlet } from 'react-router'

export interface NavItem {
  path: string
  label: string
  phase: number // build plan phase that fills this page in
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
  { path: '/users', label: 'Users', phase: 1 },
]

export default function Layout() {
  return (
    <div className="app">
      <aside className="sidebar">
        <div className="brand">Campus ERP</div>
        <nav>
          {NAV_ITEMS.map((item) => (
            <NavLink key={item.path} to={item.path}>
              {item.label}
            </NavLink>
          ))}
        </nav>
      </aside>
      <main className="content">
        <Outlet />
      </main>
    </div>
  )
}
