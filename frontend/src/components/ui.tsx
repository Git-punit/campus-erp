import { useState, type ReactNode } from 'react'
import { money } from '../lib/format'

export function ErrorBox({ message }: { message: string | null | undefined }) {
  if (!message) return null
  return <div className="error-box">{message}</div>
}

export function WarningBox({ items, onClose }: { items: string[]; onClose?: () => void }) {
  if (items.length === 0) return null
  return (
    <div className="warning-box">
      <strong>Saved, but check these clashes:</strong>
      <ul>
        {items.map((w) => (
          <li key={w}>{w}</li>
        ))}
      </ul>
      {onClose && (
        <button className="link-btn" onClick={onClose}>
          Dismiss
        </button>
      )}
    </div>
  )
}

export function Loading() {
  return <p className="muted">Loading...</p>
}

/** Percentage bar. Turns red above 100%, amber above 90%. */
export function UsageBar({ percent }: { percent: number }) {
  const cls = percent > 100 ? 'over' : percent > 90 ? 'near' : ''
  return (
    <div className="usage" title={`${percent}% used`}>
      <div className={`usage-fill ${cls}`} style={{ width: `${Math.min(percent, 100)}%` }} />
      <span className="usage-label">{percent}%</span>
    </div>
  )
}

export function Money({ value }: { value: number | null | undefined }) {
  const negative = typeof value === 'number' && value < 0
  return <span className={negative ? 'bad' : undefined}>{money(value)}</span>
}

export function Badge({ value, tone }: { value: string; tone?: string }) {
  return <span className={`badge ${tone ?? 'tone-' + value.toLowerCase()}`}>{value.replace(/_/g, ' ')}</span>
}

export interface TabDef {
  key: string
  label: string
  content: () => ReactNode
}

export function Tabs({ tabs, initial }: { tabs: TabDef[]; initial?: string }) {
  const [active, setActive] = useState(initial ?? tabs[0].key)
  const current = tabs.find((t) => t.key === active) ?? tabs[0]
  return (
    <>
      <div className="tabs">
        {tabs.map((t) => (
          <button key={t.key} className={t.key === current.key ? 'tab active' : 'tab'} onClick={() => setActive(t.key)}>
            {t.label}
          </button>
        ))}
      </div>
      {current.content()}
    </>
  )
}

export function Empty({ children }: { children: ReactNode }) {
  return <p className="muted empty">{children}</p>
}
