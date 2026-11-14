import { useEffect, useState, type FormEvent } from 'react'
import { api, ApiError } from '../api'
import { useAuth, type Role, type User } from '../auth'
import { ago, device, ONLINE_MS, OUTCOME, type Login } from '../lib/logins'

const ROLES: Role[] = ['ADMIN', 'STAFF', 'VIEWER']

interface FormState {
  username: string
  fullName: string
  role: Role
  enabled: boolean
  password: string
}

const EMPTY: FormState = { username: '', fullName: '', role: 'STAFF', enabled: true, password: '' }

export default function UsersPage() {
  const { user: me } = useAuth()
  const [users, setUsers] = useState<User[]>([])
  const [logins, setLogins] = useState<Login[]>([])
  const [error, setError] = useState<string | null>(null)
  // null = form closed, 'new' = adding, a User = editing that user
  const [editing, setEditing] = useState<User | 'new' | null>(null)

  function openForm(target: User | 'new') {
    setError(null)
    setEditing(target)
  }

  function load() {
    api
      .get<User[]>('/users')
      .then(setUsers)
      .catch((e: Error) => setError(e.message))
    api
      .get<Login[]>('/users/logins')
      .then(setLogins)
      .catch((e: Error) => setError(e.message))
  }

  const online = users.filter((u) => u.lastSeenAt && Date.now() - new Date(u.lastSeenAt).getTime() < ONLINE_MS)

  useEffect(load, [])

  async function remove(u: User) {
    if (!confirm(`Delete user "${u.username}"?`)) return
    setError(null)
    try {
      await api.delete(`/users/${u.id}`)
      load()
    } catch (e) {
      setError((e as Error).message)
    }
  }

  return (
    <>
      <div className="page-head">
        <h1>Users</h1>
        <button className="btn primary" onClick={() => openForm('new')}>
          Add user
        </button>
      </div>

      {error && <div className="error-box">{error}</div>}

      {editing && (
        <UserForm
          user={editing === 'new' ? null : editing}
          onDone={() => {
            setEditing(null)
            setError(null)
            load()
          }}
          onCancel={() => setEditing(null)}
        />
      )}

      {users.some((u) => !u.enabled) && (
        <div className="warning-box">
          Accounts marked <strong>Not active</strong> can't sign in. New sign-ups wait here: click Edit, tick the
          active box and pick their role to let them in.
        </div>
      )}
      <table className="table">
        <thead>
          <tr>
            <th>Username</th>
            <th>Name</th>
            <th>Designation</th>
            <th>Contact</th>
            <th>Role</th>
            <th>Status</th>
            <th>Last active</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {users.map((u) => (
            <tr key={u.id}>
              <td>
                {u.username}
                {u.id === me?.id && <span className="muted"> (you)</span>}
              </td>
              <td>{u.fullName}</td>
              <td>{u.designation ?? '-'}</td>
              <td className="small">{[u.email, u.phone].filter(Boolean).join(', ') || '-'}</td>
              <td>
                <span className={`badge role-${u.role.toLowerCase()}`}>{u.role}</span>
              </td>
              <td>{u.enabled ? 'Active' : <span className="badge tone-pending">Not active</span>}</td>
              <td className="small">
                {online.includes(u) ? <span className="badge tone-active">Online</span> : ago(u.lastSeenAt)}
              </td>
              <td className="actions">
                <button className="link-btn" onClick={() => openForm(u)}>
                  Edit
                </button>
                <button className="link-btn danger" onClick={() => remove(u)}>
                  Delete
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      <h2>Sign-in history</h2>
      <p className="muted small">
        {online.length} online now (active in the last 5 minutes). The latest 200 sign-in attempts, newest first.
      </p>
      <table className="table">
        <thead>
          <tr>
            <th>When</th>
            <th>User</th>
            <th>Result</th>
            <th>Device</th>
            <th>IP address</th>
          </tr>
        </thead>
        <tbody>
          {logins.map((l, i) => (
            <tr key={i}>
              <td className="small">{ago(l.at)}</td>
              <td>
                {l.fullName ?? <span className="muted">no such account</span>}
                <div className="small muted">{l.username}</div>
              </td>
              <td>
                <span className={`badge ${OUTCOME[l.outcome][1]}`}>{OUTCOME[l.outcome][0]}</span>
              </td>
              <td className="small" title={l.userAgent ?? ''}>
                {device(l.userAgent)}
              </td>
              <td className="small">{l.ip ?? '-'}</td>
            </tr>
          ))}
          {logins.length === 0 && (
            <tr>
              <td colSpan={5} className="muted">
                No sign-ins recorded yet.
              </td>
            </tr>
          )}
        </tbody>
      </table>
    </>
  )
}

function UserForm({ user, onDone, onCancel }: { user: User | null; onDone: () => void; onCancel: () => void }) {
  const [form, setForm] = useState<FormState>(
    user ? { username: user.username, fullName: user.fullName, role: user.role, enabled: user.enabled, password: '' } : EMPTY,
  )
  const [error, setError] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [busy, setBusy] = useState(false)

  function set<K extends keyof FormState>(key: K, value: FormState[K]) {
    setForm((f) => ({ ...f, [key]: value }))
  }

  async function submit(e: FormEvent) {
    e.preventDefault()
    setBusy(true)
    setError(null)
    setFieldErrors({})
    try {
      if (user) {
        await api.put(`/users/${user.id}`, {
          fullName: form.fullName,
          role: form.role,
          enabled: form.enabled,
          password: form.password || null,
        })
      } else {
        await api.post('/users', {
          username: form.username,
          password: form.password,
          fullName: form.fullName,
          role: form.role,
        })
      }
      onDone()
    } catch (err) {
      setError((err as Error).message)
      if (err instanceof ApiError) setFieldErrors(err.fieldErrors)
    } finally {
      setBusy(false)
    }
  }

  return (
    <form className="card form" onSubmit={submit}>
      <h2>{user ? `Edit ${user.username}` : 'New user'}</h2>
      {error && <div className="error-box">{error}</div>}
      <div className="form-grid">
        {!user && (
          <label>
            Username
            <input value={form.username} onChange={(e) => set('username', e.target.value)} required />
            {fieldErrors.username && <span className="field-error">{fieldErrors.username}</span>}
          </label>
        )}
        <label>
          Full name
          <input value={form.fullName} onChange={(e) => set('fullName', e.target.value)} required />
          {fieldErrors.fullName && <span className="field-error">{fieldErrors.fullName}</span>}
        </label>
        <label>
          Role
          <select value={form.role} onChange={(e) => set('role', e.target.value as Role)}>
            {ROLES.map((r) => (
              <option key={r} value={r}>
                {r}
              </option>
            ))}
          </select>
        </label>
        <label>
          {user ? 'New password (leave blank to keep)' : 'Password'}
          <input
            type="password"
            value={form.password}
            onChange={(e) => set('password', e.target.value)}
            required={!user}
            minLength={6}
            autoComplete="new-password"
          />
          {fieldErrors.password && <span className="field-error">{fieldErrors.password}</span>}
        </label>
        {user && (
          <label className="checkbox">
            <input type="checkbox" checked={form.enabled} onChange={(e) => set('enabled', e.target.checked)} />
            Account active
          </label>
        )}
      </div>
      <div className="form-actions">
        <button className="btn primary" type="submit" disabled={busy}>
          {busy ? 'Saving...' : 'Save'}
        </button>
        <button className="btn" type="button" onClick={onCancel}>
          Cancel
        </button>
      </div>
    </form>
  )
}
