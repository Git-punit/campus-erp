const rupeeFormat = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 })

/** ₹5,00,000 */
export function money(n: number | null | undefined): string {
  if (n === null || n === undefined) return '-'
  return rupeeFormat.format(n)
}

/** 6 Nov 2026 */
export function date(iso: string | null | undefined): string {
  if (!iso) return '-'
  const [y, m, d] = iso.split('-').map(Number)
  return new Date(y, m - 1, d).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })
}

/** "09:00:00" or "09:00" -> "09:00" */
export function time(t: string | null | undefined): string {
  return t ? t.slice(0, 5) : ''
}

/** MONDAY -> Mon */
export function day(d: string): string {
  return d.charAt(0) + d.slice(1, 3).toLowerCase()
}

/** ON_LEAVE -> On leave */
export function label(s: string | null | undefined): string {
  if (!s) return '-'
  const t = s.replace(/_/g, ' ').toLowerCase()
  return t.charAt(0).toUpperCase() + t.slice(1)
}

/** Today as yyyy-mm-dd in local time */
export function todayIso(): string {
  const d = new Date()
  return isoDate(d)
}

export function isoDate(d: Date): string {
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
}
