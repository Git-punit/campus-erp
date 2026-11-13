import { useEffect, useRef, useState } from 'react'
import { Link, NavLink, Outlet, useLocation } from 'react-router'
import Avatar from '../components/Avatar'
import ThemeToggle from '../components/ThemeToggle'
import { useAuth } from '../auth'
import SearchBox from './SearchBox'

export interface NavItem {
  path: string
  label: string
  adminOnly?: boolean
}

interface NavGroup {
  label: string
  items: NavItem[]
}

// pages grouped like sections of a tutorial site: the strip shows the groups, the sidebar the pages of one group
export const NAV_GROUPS: NavGroup[] = [
  { label: 'Home', items: [{ path: '/dashboard', label: 'Dashboard' }, { path: '/calendar', label: 'Calendar' }] },
  {
    label: 'Academics',
    items: [
      { path: '/faculty', label: 'Faculty' },
      { path: '/students', label: 'Students' },
      { path: '/courses', label: 'Courses' },
      { path: '/setup', label: 'Academic setup' },
    ],
  },
  { label: 'Finance', items: [{ path: '/budgets', label: 'Budgets' }] },
  {
    label: 'Research & outreach',
    items: [
      { path: '/projects', label: 'Research projects' },
      { path: '/training', label: 'Training programs' },
      { path: '/events', label: 'Events' },
    ],
  },
  { label: 'Admin', items: [{ path: '/users', label: 'Users', adminOnly: true }] },
]

export const NAV_ITEMS: NavItem[] = NAV_GROUPS.flatMap((g) => g.items)

const isUnder = (pathname: string, path: string) => pathname === path || pathname.startsWith(path + '/')

export default function Layout() {
  const { user, isAdmin, logout } = useAuth()
  const { pathname } = useLocation()
  const [menuOpen, setMenuOpen] = useState(false) // the slide-in page menu (phones and tablets)
  const [userOpen, setUserOpen] = useState(false) // the account panel under the avatar
  const userBox = useRef<HTMLDivElement>(null)

  // only the groups and pages this user may open
  const groups = NAV_GROUPS.map((g) => ({ ...g, items: g.items.filter((i) => !i.adminOnly || isAdmin) })).filter(
    (g) => g.items.length > 0,
  )
  const group = groups.find((g) => g.items.some((i) => isUnder(pathname, i.path)))
  const page = group?.items.find((i) => isUnder(pathname, i.path))

  // opening a page closes both menus
  useEffect(() => {
    setMenuOpen(false)
    setUserOpen(false)
  }, [pathname])

  // while the menu is open the page behind it shouldn't scroll
  useEffect(() => {
    document.body.style.overflow = menuOpen ? 'hidden' : ''
    return () => {
      document.body.style.overflow = ''
    }
  }, [menuOpen])

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
    <div className={`app${menuOpen ? ' menu-open' : ''}`}>
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

        <SearchBox />

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

      {/* phones and tablets: every page, by section, slides in from the left */}
      {menuOpen && <div className="drawer-backdrop" onClick={() => setMenuOpen(false)} aria-hidden="true" />}
      <nav id="main-nav" className={`drawer${menuOpen ? ' open' : ''}`} aria-label="Pages" aria-hidden={!menuOpen}>
        {groups.map((g) => (
          <div key={g.label} className="nav-group">
            <div className="nav-group-label">{g.label}</div>
            {g.items.map((item) => (
              <NavLink key={item.path} to={item.path} tabIndex={menuOpen ? undefined : -1}>
                {item.label}
              </NavLink>
            ))}
          </div>
        ))}
      </nav>

      {/* everything under the top bar; this is what slides aside when the phone menu opens */}
      <div className="main-area">
        {/* the strip of sections under the top bar; scrolls sideways on phones */}
        <nav className="strip" aria-label="Sections">
          {groups.map((g) => (
            <Link key={g.label} to={g.items[0].path} className={g === group ? 'active' : undefined}
              aria-current={g === group ? 'true' : undefined}>
              {g.label}
            </Link>
          ))}
        </nav>

        <div className="shell">
          {/* desktop: the pages of the current section, always on screen */}
          {group && (
            <aside className="sidebar" aria-label={`${group.label} pages`}>
              <div className="nav-group-label">{group.label}</div>
              {group.items.map((item) => (
                <NavLink key={item.path} to={item.path}>
                  {item.label}
                </NavLink>
              ))}
            </aside>
          )}

          <main className="content">
            <Crumbs pathname={pathname} group={group} page={page} />
            {/* keyed by address so each page fades in when you open it */}
            <div className="page" key={pathname}>
              <Outlet />
            </div>
          </main>
        </div>

        <footer className="footer">
          <div className="footer-cols">
            {groups.map((g) => (
              <div key={g.label}>
                <div className="nav-group-label">{g.label}</div>
                {g.items.map((item) => (
                  <Link key={item.path} to={item.path}>
                    {item.label}
                  </Link>
                ))}
              </div>
            ))}
          </div>
          <p className="small">Campus ERP · faculty, students, courses, budgets and research in one place</p>
        </footer>
      </div>
    </div>
  )
}

/** Home › Academics › Students › Details */
function Crumbs({ pathname, group, page }: { pathname: string; group?: NavGroup; page?: NavItem }) {
  const trail: { label: string; to?: string }[] = [{ label: 'Home', to: '/dashboard' }]
  if (pathname.startsWith('/account')) {
    trail.push({ label: 'My account', to: '/account' })
    if (pathname === '/account/password') trail.push({ label: 'Change password' })
  } else if (group && page) {
    if (group.label !== 'Home') trail.push({ label: group.label, to: group.items[0].path })
    trail.push({ label: page.label, to: page.path })
    if (pathname !== page.path) trail.push({ label: 'Details' })
  }
  if (trail.length < 2) return null
  const last = trail.length - 1
  return (
    <nav className="crumbs" aria-label="Breadcrumb">
      {trail.map((c, i) => (
        <span key={i}>
          {i > 0 && <span className="crumb-sep"> › </span>}
          {i < last && c.to ? <Link to={c.to}>{c.label}</Link> : <span aria-current="page">{c.label}</span>}
        </span>
      ))}
    </nav>
  )
}
