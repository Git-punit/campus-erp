import { useState, type FormEvent } from 'react'
import { Link } from 'react-router'
import { api, setToken } from '../api'
import { ErrorBox } from '../components/ui'

/** Change your own password, on its own page (opened from My account). */
export default function ChangePasswordPage() {
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
      // old logins elsewhere stop working; keep this tab logged in with the new token
      const res = await api.post<{ token: string }>('/auth/password', {
        currentPassword: form.currentPassword,
        newPassword: form.newPassword,
      })
      setToken(res.token)
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
      <p className="crumbs">
        <Link to="/account">My account</Link> /
      </p>
      <h1>Change password</h1>
      <form className="card form narrow-form" onSubmit={submit}>
        <ErrorBox message={error} />
        {done && (
          <div className="ok-box">
            Password changed. You've been logged out everywhere else. <Link to="/account">Back to My account</Link>
          </div>
        )}
        <label>
          Current password
          <input
            type="password"
            autoComplete="current-password"
            value={form.currentPassword}
            onChange={(e) => setForm({ ...form, currentPassword: e.target.value })}
            autoFocus
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
          <Link className="btn" to="/account">
            Cancel
          </Link>
        </div>
      </form>
    </>
  )
}
