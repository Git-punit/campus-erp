import { Fragment, useState } from 'react'
import { api } from '../api'
import { downloadCsv } from '../lib/csv'
import { ErrorBox } from './ui'

/**
 * Upload a CSV file of records. The server checks every row first and saves
 * nothing if any row is wrong, so a failed upload can simply be fixed and retried.
 */
export default function ImportBox({
  what,
  endpoint,
  required,
  optional,
  example,
  onDone,
  onClose,
}: {
  what: string // "students"
  endpoint: string // "/students/import"
  required: string[]
  optional: string[]
  example: Record<string, string>
  onDone: () => void
  onClose: () => void
}) {
  const [error, setError] = useState<string | null>(null)
  const [result, setResult] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  async function upload(file: File) {
    setBusy(true)
    setError(null)
    setResult(null)
    try {
      const r = await api.postText<{ imported: number }>(endpoint, await file.text())
      setResult(`Imported ${r.imported} ${what} from ${file.name}.`)
      onDone()
    } catch (e) {
      setError((e as Error).message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="card form import-box">
      <div className="section-head">
        <h2>Import {what} from a CSV file</h2>
        <span className="spacer" />
        <button className="link-btn" type="button" onClick={onClose}>
          Close
        </button>
      </div>
      <p className="small">
        The first line must name the columns. Required: {codes(required)}. Optional: {codes(optional)}. In Excel, use
        File, Save As, CSV.
      </p>
      <p className="small muted">
        Every row is checked before anything is saved. If one row is wrong, nothing is imported and the rows to fix
        are listed.
      </p>
      <ErrorBox message={error} />
      {result && <div className="ok-box">{result}</div>}
      <div className="form-actions">
        <label className={`btn primary file-btn${busy ? ' disabled' : ''}`}>
          {busy ? 'Importing...' : 'Choose CSV file'}
          <input
            type="file"
            accept=".csv,text/csv"
            disabled={busy}
            onChange={(e) => {
              const file = e.target.files?.[0]
              e.target.value = '' // so choosing the same file again re-uploads it
              if (file) upload(file)
            }}
          />
        </label>
        <button
          className="btn"
          type="button"
          onClick={() =>
            downloadCsv(`${what}-template`, [Object.fromEntries([...required, ...optional].map((c) => [c, example[c] ?? '']))])
          }
        >
          Download template
        </button>
      </div>
    </div>
  )
}

function codes(names: string[]) {
  return names.map((c, i) => (
    <Fragment key={c}>
      {i > 0 && ', '}
      <code>{c}</code>
    </Fragment>
  ))
}
