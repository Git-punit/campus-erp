import { useEffect, useRef, useState } from 'react'
import { NavLink, Outlet, useLocation } from 'react-router'
import Avatar from '../components/Avatar'
import ThemeToggle from '../components/ThemeToggle'
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
  const { pathname } = useLocation()
  const [menuOpen, setMenuOpen] = useState(false) // the side panel with the page links
  const [userOpen, setUserOpen] = useState(false) // the account panel under the avatar
  const userBox = useRef<HTMLDivElement>(null)

  // opening a page closes both menus
  useEffect(() => {
    setMenuOpen(false)
    setUserOpen(false)
  }, [pathname])

  // a click outside the account panel, or Escape, closes it (and the page menu)
  useEffect(() => {
    if (!userOpen && !menuOpen) return
    const onClick = (e: MouseEvent) => {
      if (userBox.current && !userBox.current.contains(e.target as Node)) setUserOpen(false)
    }
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        setUserOpen(false)
        setMenuOpen(false)
      }
    }
    document.addEventListener('mousedown', onClick)
    document.addEventListener('keydown', onKey)
    return () => {
      document.removeEventListener('mousedown', onClick)
      document.removeEventListener('keydown', onKey)
    }
  }, [userOpen, menuOpen])

  return (
    <div className="app">
      <header className="topbar">
        <button
          type="button"
          className="menu-btn"
          aria-label={menuOpen ? 'Close menu' : 'Open menu'}
          aria-expanded={menuOpen}
          aria-controls="main-nav"
          onClick={() => setMenuOpen((o) => !o)}
        >
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"
            strokeLinecap="round" aria-hidden="true">
            {menuOpen ? <path d="M6 6l12 12M18 6L6 18" /> : <path d="M4 7h16M4 12h16M4 17h16" />}
          </svg>
        </button>

        <NavLink to="/dashboard" className="brand">
          Campus ERP
        </NavLink>

        <div className="topbar-right">
          <ThemeToggle />
          {user && (
            <div className="user-menu" ref={userBox}>
              <button
                type="button"
                className="user-btn"
                aria-haspopup="true"
                aria-expanded={userOpen}
                aria-label={`Account: ${user.fullName}`}
                onClick={() => setUserOpen((o) => !o)}
              >
                <Avatar user={user} size={32} />
              </button>
              {userOpen && (
                <div className="user-panel">
                  <div className="sidebar-name">
                    <div className="sidebar-avatar">
                      <Avatar user={user} size={40} />
                      <div className="role">{user.role.toLowerCase()}</div>
                    </div>
                    <span>{user.fullName}</span>
                  </div>
                  <NavLink to="/account" className="link-btn">
                    My account
                  </NavLink>
                  <button type="button" className="link-btn" onClick={logout}>
                    Log out
                  </button>
                </div>
              )}
            </div>
          )}
        </div>
      </header>

      {/* the page links slide in from the left; clicking outside closes them */}
      {menuOpen && <div className="drawer-backdrop" onClick={() => setMenuOpen(false)} aria-hidden="true" />}
      <nav id="main-nav" className={`drawer${menuOpen ? ' open' : ''}`} aria-label="Pages" aria-hidden={!menuOpen}>
        {NAV_ITEMS.filter((item) => !item.adminOnly || isAdmin).map((item) => (
          <NavLink key={item.path} to={item.path} tabIndex={menuOpen ? undefined : -1}>
            {item.label}
          </NavLink>
        ))}
      </nav>

      <main className="content">
        {/* keyed by address so each page fades in when you open it */}
        <div className="page" key={pathname}>
          <Outlet />
        </div>
      </main>
    </div>
  )
}
