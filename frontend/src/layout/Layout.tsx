import { useEffect, useRef, useState } from 'react'
import { NavLink, Outlet, useLocation } from 'react-router'
import Avatar from '../components/Avatar'
import ThemeToggle from '../components/ThemeToggle'
import { useAuth } from '../auth'

export interface NavItem {
  path: string
  label: string
  short?: string // shorter name for the top bar on wide screens
  adminOnly?: boolean
}

export const NAV_ITEMS: NavItem[] = [
  { path: '/dashboard', label: 'Dashboard' },
  { path: '/faculty', label: 'Faculty' },
  { path: '/courses', label: 'Courses' },
  { path: '/students', label: 'Students' },
  { path: '/calendar', label: 'Calendar' },
  { path: '/budgets', label: 'Budgets' },
  { path: '/projects', label: 'Research projects', short: 'Projects' },
  { path: '/training', label: 'Training programs', short: 'Training' },
  { path: '/events', label: 'Events' },
  { path: '/setup', label: 'Academic setup', short: 'Setup' },
  { path: '/users', label: 'Users', adminOnly: true },
]

export default function Layout() {
  const { user, isAdmin, logout } = useAuth()
  const { pathname } = useLocation()
  const [menuOpen, setMenuOpen] = useState(false) // the page links, on narrow screens
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
        <NavLink to="/dashboard" className="brand">
          Campus ERP
        </NavLink>

        <nav id="main-nav" className={`topnav${menuOpen ? ' open' : ''}`} aria-label="Pages">
          {NAV_ITEMS.filter((item) => !item.adminOnly || isAdmin).map((item) => (
            <NavLink key={item.path} to={item.path} title={item.label}>
              <span className="label-short">{item.short ?? item.label}</span>
              <span className="label-full">{item.label}</span>
            </NavLink>
          ))}
        </nav>

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
        </div>
      </header>

      <main className="content">
        {/* keyed by address so each page fades in when you open it */}
        <div className="page" key={pathname}>
          <Outlet />
        </div>
      </main>
    </div>
  )
}
