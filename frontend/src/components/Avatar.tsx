import { useEffect, useState } from 'react'
import { getToken } from '../api'
import type { User } from '../auth'

/** Your profile photo, or your initials when there's none. */
export default function Avatar({ user, size = 36 }: { user: User; size?: number }) {
  const [url, setUrl] = useState<string | null>(null)

  // the photo needs the login token, so it's fetched rather than linked; user changes after an upload
  useEffect(() => {
    if (!user.hasPhoto) {
      setUrl(null)
      return
    }
    let objectUrl: string | null = null
    fetch('/api/auth/photo', { headers: { Authorization: `Bearer ${getToken()}` } })
      .then((r) => (r.ok ? r.blob() : null))
      .then((blob) => {
        if (blob) {
          objectUrl = URL.createObjectURL(blob)
          setUrl(objectUrl)
        }
      })
      .catch(() => setUrl(null))
    return () => {
      if (objectUrl) URL.revokeObjectURL(objectUrl)
    }
  }, [user])

  const initials = user.fullName
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((w) => w[0].toUpperCase())
    .join('')
  return url ? (
    <img className="avatar" src={url} alt={user.fullName} style={{ width: size, height: size }} />
  ) : (
    <span className="avatar initials" aria-hidden="true" style={{ width: size, height: size, fontSize: size * 0.4 }}>
      {initials}
    </span>
  )
}
