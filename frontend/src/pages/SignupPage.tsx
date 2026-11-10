import { useState, type FormEvent } from 'react'
import { Link, Navigate } from 'react-router'
import { api } from '../api'
import { useAuth } from '../auth'

/** Ask for an account: name, email and password only. An admin approves it and picks the role. */
export default function SignupPage() {
  const { user } = useAuth()
  const [form, setForm] = useState({ name: '', email: '', password: '', repeat: '' })
  const [error, setError] = useState<string | null>(null)
  const [done, setDone] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  if (user) return <Navigate to="/dashboard" replace />

  async function submit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    if (form.password !== form.repeat) {
      setError("The two passwords don't match")
      return
    }
    setBusy(true)
    try {
      // exactly these three fields; the server refuses anything else
      const res = await api.post<{ message: string }>('/auth/signup', {
        name: form.name.trim(),
        email: form.email.trim(),
        password: form.password,
      })
      setDone(res.message)
    } catch (err) {
      setError((err as Error).message)
    } finally {
      setBusy(false)
    }
  }

  if (done) {
    return (
      <div className="login-page">
        <div className="card login-card">
          <h1>Request sent</h1>
          <div className="ok-box">{done}</div>
          <p className="small muted">Once it's approved, sign in with your email and password.</p>
          <Link className="btn primary" to="/login">
            Back to sign in
          </Link>
        </div>
      </div>
    )
  }

  return (
    <div className="login-page">
      <form className="card login-card" onSubmit={submit}>
        <h1>Create an account</h1>
        <p className="muted">An admin approves new accounts before they can sign in.</p>
        {error && <div className="error-box">{error}</div>}
        <label>
          Full name
          <input
            value={form.name}
            maxLength={100}
            onChange={(e) => setForm({ ...form, name: e.target.value })}
            autoFocus
            autoComplete="name"
            required
          />
        </label>
        <label>
          Email
          <input
            type="email"
            value={form.email}
            maxLength={100}
            onChange={(e) => setForm({ ...form, email: e.target.value })}
            autoComplete="email"
            required
          />
        </label>
        <label>
          Password (6+ characters)
          <input
            type="password"
            value={form.password}
            minLength={6}
            maxLength={100}
            onChange={(e) => setForm({ ...form, password: e.target.value })}
            autoComplete="new-password"
            required
          />
        </label>
        <label>
          Password again
          <input
            type="password"
            value={form.repeat}
            onChange={(e) => setForm({ ...form, repeat: e.target.value })}
            autoComplete="new-password"
            required
          />
        </label>
        <button className="btn primary" type="submit" disabled={busy}>
          {busy ? 'Sending...' : 'Create account'}
        </button>
        <p className="small center">
          Already have an account? <Link to="/login">Sign in</Link>
        </p>
      </form>
    </div>
  )
}
