import { useState, type FormEvent, type ReactNode } from 'react'
import { api, ApiError } from '../api'
import { useAuth } from '../auth'
import { useLoad } from '../lib/useLoad'
import { Empty, ErrorBox, Loading } from './ui'

/**
 * A list with add / edit / delete, driven by a small config. Used for the
 * simpler records (departments, courses, faculty, students, events...).
 */

export type FieldType = 'text' | 'email' | 'number' | 'date' | 'select' | 'textarea' | 'checkbox'

export interface FieldDef {
  name: string
  label: string
  type?: FieldType
  required?: boolean
  options?: { value: string | number; label: string }[]
  placeholder?: string
  wide?: boolean
}

export interface Column<T> {
  label: string
  render: (row: T) => ReactNode
  className?: string
}

export type FormValues = Record<string, string | boolean>

export interface CrudConfig<T extends { id: number }> {
  noun: string // "department"
  listPath: string // GET path, may include filters
  savePath: string // POST to this, PUT to savePath/{id}
  columns: Column<T>[]
  fields: FieldDef[]
  empty: FormValues
  toForm: (row: T) => FormValues
  // turn form strings into the request body; default: send as-is with numbers parsed
  toRequest?: (form: FormValues) => unknown
  canDelete?: boolean
  headerExtra?: ReactNode
  rowActions?: (row: T) => ReactNode
  emptyText?: string
}

export default function CrudSection<T extends { id: number }>({ config }: { config: CrudConfig<T> }) {
  const { canEdit } = useAuth()
  const { data, error, loading, reload } = useLoad<T[]>(config.listPath)
  const [editing, setEditing] = useState<T | 'new' | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)

  async function remove(row: T) {
    if (!confirm(`Delete this ${config.noun}?`)) return
    setActionError(null)
    try {
      await api.delete(`${config.savePath}/${row.id}`)
      reload()
    } catch (e) {
      setActionError((e as Error).message)
    }
  }

  return (
    <>
      <div className="section-head">
        {config.headerExtra}
        <span className="spacer" />
        {canEdit && (
          <button
            className="btn primary"
            onClick={() => {
              setActionError(null)
              setEditing('new')
            }}
          >
            Add {config.noun}
          </button>
        )}
      </div>
      <ErrorBox message={error ?? actionError} />
      {editing && (
        <RecordForm
          config={config}
          row={editing === 'new' ? null : editing}
          onDone={() => {
            setEditing(null)
            reload()
          }}
          onCancel={() => setEditing(null)}
        />
      )}
      {loading && !data ? (
        <Loading />
      ) : data && data.length === 0 ? (
        <Empty>{config.emptyText ?? `No ${config.noun}s yet.`}</Empty>
      ) : (
        <table className="table">
          <thead>
            <tr>
              {config.columns.map((c) => (
                <th key={c.label} className={c.className}>
                  {c.label}
                </th>
              ))}
              {(canEdit || config.rowActions) && <th></th>}
            </tr>
          </thead>
          <tbody>
            {data?.map((row) => (
              <tr key={row.id}>
                {config.columns.map((c) => (
                  <td key={c.label} className={c.className}>
                    {c.render(row)}
                  </td>
                ))}
                {(canEdit || config.rowActions) && (
                  <td className="actions">
                    {config.rowActions?.(row)}
                    {canEdit && (
                      <button className="link-btn" onClick={() => setEditing(row)}>
                        Edit
                      </button>
                    )}
                    {canEdit && config.canDelete !== false && (
                      <button className="link-btn danger" onClick={() => remove(row)}>
                        Delete
                      </button>
                    )}
                  </td>
                )}
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </>
  )
}

function defaultRequest(fields: FieldDef[], form: FormValues) {
  const out: Record<string, unknown> = {}
  for (const f of fields) {
    const v = form[f.name]
    if (f.type === 'checkbox') out[f.name] = Boolean(v)
    else if (v === '' || v === undefined) out[f.name] = null
    else if (f.type === 'number') out[f.name] = Number(v)
    else out[f.name] = v
  }
  return out
}

function RecordForm<T extends { id: number }>({
  config,
  row,
  onDone,
  onCancel,
}: {
  config: CrudConfig<T>
  row: T | null
  onDone: () => void
  onCancel: () => void
}) {
  const [form, setForm] = useState<FormValues>(row ? config.toForm(row) : config.empty)
  const [error, setError] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [busy, setBusy] = useState(false)

  async function submit(e: FormEvent) {
    e.preventDefault()
    setBusy(true)
    setError(null)
    setFieldErrors({})
    const body = config.toRequest ? config.toRequest(form) : defaultRequest(config.fields, form)
    try {
      if (row) await api.put(`${config.savePath}/${row.id}`, body)
      else await api.post(config.savePath, body)
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
      <h2>{row ? `Edit ${config.noun}` : `New ${config.noun}`}</h2>
      <ErrorBox message={error} />
      <div className="form-grid">
        {config.fields.map((f) => (
          <Field
            key={f.name}
            def={f}
            value={form[f.name]}
            error={fieldErrors[f.name]}
            onChange={(v) => setForm((old) => ({ ...old, [f.name]: v }))}
          />
        ))}
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

export function Field({
  def,
  value,
  error,
  onChange,
}: {
  def: FieldDef
  value: string | boolean | undefined
  error?: string
  onChange: (v: string | boolean) => void
}) {
  if (def.type === 'checkbox') {
    return (
      <label className="checkbox">
        <input type="checkbox" checked={Boolean(value)} onChange={(e) => onChange(e.target.checked)} />
        {def.label}
      </label>
    )
  }
  const str = typeof value === 'string' ? value : ''
  let input: ReactNode
  if (def.type === 'select') {
    input = (
      <select value={str} onChange={(e) => onChange(e.target.value)} required={def.required}>
        <option value="">Choose...</option>
        {def.options?.map((o) => (
          <option key={o.value} value={String(o.value)}>
            {o.label}
          </option>
        ))}
      </select>
    )
  } else if (def.type === 'textarea') {
    input = <textarea value={str} rows={3} onChange={(e) => onChange(e.target.value)} required={def.required} />
  } else {
    input = (
      <input
        type={def.type ?? 'text'}
        value={str}
        placeholder={def.placeholder}
        onChange={(e) => onChange(e.target.value)}
        required={def.required}
        step={def.type === 'number' ? 'any' : undefined}
      />
    )
  }
  return (
    <label className={def.wide ? 'wide' : undefined}>
      {def.label}
      {input}
      {error && <span className="field-error">{error}</span>}
    </label>
  )
}

/** Helpers for building select options */
export function optionsOf<T extends { id: number }>(rows: T[] | null, text: (r: T) => string) {
  return (rows ?? []).map((r) => ({ value: r.id, label: text(r) }))
}

export function enumOptions(values: string[]) {
  return values.map((v) => ({ value: v, label: v.replace(/_/g, ' ').toLowerCase().replace(/^./, (c) => c.toUpperCase()) }))
}

/** "" for null/undefined, otherwise String(v) */
export function str(v: unknown): string {
  return v === null || v === undefined ? '' : String(v)
}
