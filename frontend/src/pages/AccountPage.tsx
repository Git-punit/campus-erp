import { useState, type FormEvent } from 'react'
import { api } from '../api'
import { useAuth } from '../auth'
import { ErrorBox } from '../components/ui'

/** Your own account: change your password. */
export default function AccountPage() {
  const { user } = useAuth()
  const [form, setForm] = useState({ currentPassword: '', newPassword: '', repeat: '' })
  const [error, setError] = useState<string | null>(null)
  const [done, setDone] = useState(false)
  const [busy, setBusy] = useState(false)

  async function submit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setDone(false)
    if (form.newPassword !== form.repeat) {
      setError("The two new passwords don't match")
      return
    }
    setBusy(true)
    try {
      await api.post('/auth/password', { currentPassword: form.currentPassword, newPassword: form.newPassword })
      setForm({ currentPassword: '', newPassword: '', repeat: '' })
      setDone(true)
    } catch (err) {
      setError((err as Error).message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      <h1>My account</h1>
      <div className="card facts">
        <div><span>Name</span>{user?.fullName}</div>
        <div><span>Username</span>{user?.username}</div>
        <div><span>Role</span>{user?.role.toLowerCase()}</div>
      </div>
      <form className="card form narrow-form" onSubmit={submit}>
        <h2>Change password</h2>
        <ErrorBox message={error} />
        {done && <div className="ok-box">Password changed. Use the new one next time you log in.</div>}
        <label>
          Current password
          <input
            type="password"
            autoComplete="current-password"
            value={form.currentPassword}
            onChange={(e) => setForm({ ...form, currentPassword: e.target.value })}
            required
          />
        </label>
        <label>
          New password (6+ characters)
          <input
            type="password"
            autoComplete="new-password"
            minLength={6}
            value={form.newPassword}
            onChange={(e) => setForm({ ...form, newPassword: e.target.value })}
            required
          />
        </label>
        <label>
          New password again
          <input
            type="password"
            autoComplete="new-password"
            minLength={6}
            value={form.repeat}
            onChange={(e) => setForm({ ...form, repeat: e.target.value })}
            required
          />
        </label>
        <div className="form-actions">
          <button className="btn primary" type="submit" disabled={busy}>
            {busy ? 'Saving...' : 'Change password'}
          </button>
        </div>
      </form>
    </>
  )
}
