// Sign-in history helpers, shared by the Users page (everyone) and My account (your own)

export interface Login {
  at: string
  username: string
  fullName: string | null
  outcome: 'SUCCESS' | 'WRONG_PASSWORD' | 'LOCKED' | 'NOT_ACTIVE'
  ip: string | null
  userAgent: string | null
}

export const OUTCOME: Record<Login['outcome'], [string, string]> = {
  SUCCESS: ['Signed in', 'tone-active'],
  WRONG_PASSWORD: ['Wrong password', 'tone-rejected'],
  LOCKED: ['Locked out', 'tone-rejected'],
  NOT_ACTIVE: ['Not approved', 'tone-pending'],
}

// someone who used the app in the last 5 minutes counts as online
export const ONLINE_MS = 5 * 60_000

/** "just now", "12 min ago", "3 h ago", or the date and time */
export function ago(iso: string | null): string {
  if (!iso) return 'Never'
  const ms = Date.now() - new Date(iso).getTime()
  if (ms < 60_000) return 'just now'
  if (ms < 3_600_000) return `${Math.floor(ms / 60_000)} min ago`
  if (ms < 86_400_000) return `${Math.floor(ms / 3_600_000)} h ago`
  return new Date(iso).toLocaleString('en-IN', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' })
}

/** "Chrome on Mac" from a browser's user-agent text */
export function device(ua: string | null): string {
  if (!ua) return '-'
  const browser = /Edg\//.test(ua) ? 'Edge' : /Chrome\//.test(ua) ? 'Chrome' : /Firefox\//.test(ua) ? 'Firefox'
    : /Safari\//.test(ua) ? 'Safari' : 'Other browser'
  const os = /iPhone|iPad/.test(ua) ? 'iPhone/iPad' : /Android/.test(ua) ? 'Android' : /Mac OS/.test(ua) ? 'Mac'
    : /Windows/.test(ua) ? 'Windows' : /Linux/.test(ua) ? 'Linux' : ''
  return os ? `${browser} on ${os}` : browser
}
