import { useState, type FormEvent } from 'react'
import { api, setToken } from '../api'
import type { User } from '../auth'
import { useAuth } from '../auth'
import Avatar from '../components/Avatar'
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
      <h1>My account</h1>
      <div className="card facts account-head">
        {user && <Avatar user={user} size={56} />}
        <div><span>Name</span>{user?.fullName}</div>
        <div><span>Username</span>{user?.username}</div>
        <div><span>Role</span>{user?.role.toLowerCase()}</div>
        {user?.designation && <div><span>Designation</span>{user.designation}</div>}
        {user?.email && <div><span>Email</span>{user.email}</div>}
        {user?.phone && <div><span>Phone</span>{user.phone}</div>}
      </div>
      <ProfileForm />
      <PhotoCard />
      <UsernameForm />
      <form className="card form narrow-form" onSubmit={submit}>
        <h2>Change password</h2>
        <ErrorBox message={error} />
        {done && <div className="ok-box">Password changed. You've been logged out everywhere else.</div>}
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
      <LogoutOthers />
    </>
  )
}

/** Name, contact details and designation. */
function ProfileForm() {
  const { user, setUser } = useAuth()
  const [form, setForm] = useState({
    fullName: user?.fullName ?? '',
    email: user?.email ?? '',
    phone: user?.phone ?? '',
    designation: user?.designation ?? '',
  })
  const [error, setError] = useState<string | null>(null)
  const [done, setDone] = useState(false)

  async function submit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setDone(false)
    try {
      setUser(await api.put<User>('/auth/me', form))
      setDone(true)
    } catch (err) {
      setError((err as Error).message)
    }
  }

  const field = (key: keyof typeof form, label: string, props: Record<string, unknown> = {}) => (
    <label>
      {label}
      <input value={form[key]} onChange={(e) => setForm({ ...form, [key]: e.target.value })} {...props} />
    </label>
  )

  return (
    <form className="card form narrow-form" onSubmit={submit}>
      <h2>Your details</h2>
      <ErrorBox message={error} />
      {done && <div className="ok-box">Details saved.</div>}
      {field('fullName', 'Full name', { maxLength: 100, required: true })}
      {field('designation', 'Designation / department (optional)', {
        maxLength: 100,
        placeholder: 'e.g. Accounts Officer, Finance',
      })}
      {field('email', 'Email (optional)', { type: 'email', maxLength: 100, autoComplete: 'email' })}
      {field('phone', 'Phone (optional)', { type: 'tel', maxLength: 20, autoComplete: 'tel' })}
      <div className="form-actions">
        <button className="btn primary" type="submit">
          Save details
        </button>
      </div>
    </form>
  )
}

/** Upload, replace or remove your profile photo. */
function PhotoCard() {
  const { user, setUser } = useAuth()
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  async function upload(file: File) {
    setError(null)
    setBusy(true)
    try {
      setUser(await api.upload<User>('/auth/photo', file))
    } catch (err) {
      setError((err as Error).message)
    } finally {
      setBusy(false)
    }
  }

  async function remove() {
    if (!confirm('Remove your photo?')) return
    setError(null)
    try {
      setUser(await api.delete<User>('/auth/photo'))
    } catch (err) {
      setError((err as Error).message)
    }
  }

  if (!user) return null
  return (
    <div className="card form narrow-form">
      <h2>Photo</h2>
      <ErrorBox message={error} />
      <div className="photo-row">
        <Avatar user={user} size={72} />
        <div className="form-actions">
          <label className={`btn file-btn${busy ? ' disabled' : ''}`}>
            {busy ? 'Uploading...' : user.hasPhoto ? 'Change photo' : 'Upload photo'}
            <input
              type="file"
              accept="image/jpeg,image/png"
              disabled={busy}
              onChange={(e) => {
                const file = e.target.files?.[0]
                e.target.value = ''
                if (file) upload(file)
              }}
            />
          </label>
          {user.hasPhoto && (
            <button className="link-btn danger" type="button" onClick={remove}>
              Remove
            </button>
          )}
        </div>
      </div>
      <p className="small muted">JPG or PNG, up to 2 MB.</p>
    </div>
  )
}

/** Change the name you log in with. */
function UsernameForm() {
  const { user, setUser } = useAuth()
  const [form, setForm] = useState({ newUsername: '', currentPassword: '' })
  const [error, setError] = useState<string | null>(null)
  const [done, setDone] = useState<string | null>(null)

  async function submit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setDone(null)
    try {
      const res = await api.post<{ token: string; user: User }>('/auth/username', form)
      setToken(res.token) // the old token named the old username
      setUser(res.user)
      setForm({ newUsername: '', currentPassword: '' })
      setDone(res.user.username)
    } catch (err) {
      setError((err as Error).message)
    }
  }

  return (
    <form className="card form narrow-form" onSubmit={submit}>
      <h2>Username</h2>
      <p className="small muted">
        You log in as <strong>{user?.username}</strong>. Changing it logs you out on other devices. Past records keep
        the old name.
      </p>
      <ErrorBox message={error} />
      {done && <div className="ok-box">Username changed. Log in as {done} from now on.</div>}
      <label>
        New username
        <input
          value={form.newUsername}
          minLength={3}
          maxLength={50}
          pattern="[a-zA-Z0-9._\-]+"
          title="Letters, numbers, dot, dash and underscore"
          autoComplete="off"
          onChange={(e) => setForm({ ...form, newUsername: e.target.value })}
          required
        />
      </label>
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
      <div className="form-actions">
        <button className="btn primary" type="submit">
          Change username
        </button>
      </div>
    </form>
  )
}

/** End logins on other computers and phones, e.g. a shared computer you forgot to log out of. */
function LogoutOthers() {
  const [error, setError] = useState<string | null>(null)
  const [done, setDone] = useState(false)

  async function run() {
    if (!confirm('Log out of this account on every other computer and phone?')) return
    setError(null)
    try {
      const res = await api.post<{ token: string }>('/auth/logout-others')
      setToken(res.token) // this tab stays logged in
      setDone(true)
    } catch (err) {
      setError((err as Error).message)
    }
  }

  return (
    <div className="card form narrow-form">
      <h2>Other devices</h2>
      <p className="small muted">
        Forgot to log out somewhere? This ends every login of your account except this one. Your password stays the
        same.
      </p>
      <ErrorBox message={error} />
      {done && <div className="ok-box">Logged out everywhere else.</div>}
      <div className="form-actions">
        <button className="btn" type="button" onClick={run}>
          Log out of all other devices
        </button>
      </div>
    </div>
  )
}
