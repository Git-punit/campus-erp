import { Fragment, useState, type FormEvent } from 'react'
import { Link, useParams } from 'react-router'
import { api } from '../api'
import { useAuth } from '../auth'
import { Badge, Empty, ErrorBox, Loading, Money } from '../components/ui'
import { date, label } from '../lib/format'
import type { Acceptance, TrainingClient, TrainingProgram } from '../lib/types'
import { useLoad } from '../lib/useLoad'

export default function TrainingProgramPage() {
  const { id } = useParams()
  const { canEdit } = useAuth()
  const program = useLoad<TrainingProgram>(`/training-programs/${id}`)
  const clients = useLoad<TrainingClient[]>(`/training-programs/${id}/clients`)
  const [editing, setEditing] = useState<TrainingClient | 'new' | null>(null)
  const [historyOf, setHistoryOf] = useState<number | null>(null)
  const [error, setError] = useState<string | null>(null)

  if (program.error) return <ErrorBox message={program.error} />
  if (!program.data) return <Loading />
  const p = program.data

  function refresh() {
    program.reload()
    clients.reload()
  }

  async function remove(c: TrainingClient) {
    if (!confirm(`Remove ${c.clientName}?`)) return
    setError(null)
    try {
      await api.delete(`/training-clients/${c.id}`)
      refresh()
    } catch (e) {
      setError((e as Error).message)
    }
  }

  return (
    <>
      <h1>{p.name}</h1>
      {p.description && <p>{p.description}</p>}
      <div className="card facts">
        <div><span>Duration</span>{p.durationDays ? `${p.durationDays} days` : '-'}</div>
        <div><span>Coordinator</span>{p.coordinator ? <Link to={`/faculty/${p.coordinator.id}`}>{p.coordinator.name}</Link> : '-'}</div>
        <div><span>Clients reached</span>{p.clientsReached}</div>
        <div><span>Proposals</span>{p.proposals}</div>
        <div><span>Accepted</span>{p.accepted}</div>
        <div><span>Conducted</span>{p.conducted}</div>
        <div><span>Fee (accepted)</span><Money value={p.totalFee} /></div>
        <div><span>Actual cost</span><Money value={p.totalCost} /></div>
      </div>

      <div className="section-head">
        <h2>Clients</h2>
        <span className="spacer" />
        {canEdit && (
          <button className="btn primary" onClick={() => setEditing('new')}>
            Add client
          </button>
        )}
      </div>
      <ErrorBox message={error ?? clients.error} />
      {editing && (
        <ClientForm
          programId={p.id}
          client={editing === 'new' ? null : editing}
          onDone={() => {
            setEditing(null)
            refresh()
          }}
          onCancel={() => setEditing(null)}
        />
      )}
      {!clients.data ? (
        <Loading />
      ) : clients.data.length === 0 ? (
        <Empty>No clients yet.</Empty>
      ) : (
        <table className="table">
          <thead>
            <tr>
              <th>Client</th>
              <th>Contacted</th>
              <th>Proposal</th>
              <th>Accepted</th>
              <th>Conducted</th>
              <th className="num">Budget required</th>
              <th className="num">Fee</th>
              <th className="num">Actual cost</th>
              <th className="num">Profit / loss</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {clients.data.map((c) => (
              <Fragment key={c.id}>
                <tr>
                  <td>
                    <strong>{c.clientName}</strong>
                    <div className="muted small">{[c.contactPerson, c.phone, c.email].filter(Boolean).join(', ')}</div>
                  </td>
                  <td>{date(c.dateContacted)}</td>
                  <td>{c.proposalSubmitted ? <>Yes <span className="muted small">{date(c.proposalDate)}</span></> : 'No'}</td>
                  <td>
                    {c.accepted ? <Badge value={c.accepted} tone={'tone-acc-' + c.accepted.toLowerCase()} /> : '-'}
                    {c.acceptedRemarks && <div className="muted small">{c.acceptedRemarks}</div>}
                  </td>
                  <td>
                    {c.conducted ? (
                      <>
                        Yes <div className="muted small">{date(c.conductedFrom)} to {date(c.conductedTo)}</div>
                      </>
                    ) : (
                      'No'
                    )}
                  </td>
                  <td className="num"><Money value={c.budgetRequired} /></td>
                  <td className="num"><Money value={c.feeCharged} /></td>
                  <td className="num">
                    {c.budgetId ? <Link to={`/budgets/${c.budgetId}`}><Money value={c.actualCost} /></Link> : '-'}
                  </td>
                  <td className="num">{c.profit === null ? '-' : <Money value={c.profit} />}</td>
                  <td className="actions">
                    {c.history.length > 0 && (
                      <button className="link-btn" onClick={() => setHistoryOf(historyOf === c.id ? null : c.id)}>
                        History
                      </button>
                    )}
                    {canEdit && (
                      <>
                        <button className="link-btn" onClick={() => setEditing(c)}>
                          Edit
                        </button>
                        <button className="link-btn danger" onClick={() => remove(c)}>
                          Delete
                        </button>
                      </>
                    )}
                  </td>
                </tr>
                {historyOf === c.id && (
                  <tr className="sub-row">
                    <td colSpan={10}>
                      <strong>Changes to "Accepted":</strong>
                      <ul>
                        {c.history.map((h, i) => (
                          <li key={i}>
                            {date(h.date)}: {label(h.accepted)}
                            {h.remarks ? ` (${h.remarks})` : ''}
                            {h.changedBy ? <span className="muted"> by {h.changedBy}</span> : null}
                          </li>
                        ))}
                      </ul>
                    </td>
                  </tr>
                )}
              </Fragment>
            ))}
          </tbody>
        </table>
      )}
    </>
  )
}

interface ClientForm {
  clientName: string
  contactPerson: string
  phone: string
  email: string
  dateContacted: string
  proposalSubmitted: boolean
  proposalDate: string
  accepted: '' | Acceptance
  acceptedDate: string
  acceptedRemarks: string
  conducted: boolean
  conductedFrom: string
  conductedTo: string
  budgetRequired: string
  feeCharged: string
}

function ClientForm({
  programId,
  client,
  onDone,
  onCancel,
}: {
  programId: number
  client: TrainingClient | null
  onDone: () => void
  onCancel: () => void
}) {
  const s = (v: string | number | null | undefined) => (v === null || v === undefined ? '' : String(v))
  const [f, setF] = useState<ClientForm>({
    clientName: s(client?.clientName),
    contactPerson: s(client?.contactPerson),
    phone: s(client?.phone),
    email: s(client?.email),
    dateContacted: s(client?.dateContacted),
    proposalSubmitted: client?.proposalSubmitted ?? false,
    proposalDate: s(client?.proposalDate),
    accepted: client?.accepted ?? '',
    acceptedDate: s(client?.acceptedDate),
    acceptedRemarks: s(client?.acceptedRemarks),
    conducted: client?.conducted ?? false,
    conductedFrom: s(client?.conductedFrom),
    conductedTo: s(client?.conductedTo),
    budgetRequired: s(client?.budgetRequired),
    feeCharged: s(client?.feeCharged),
  })
  const [error, setError] = useState<string | null>(null)
  const set = (patch: Partial<ClientForm>) => setF((old) => ({ ...old, ...patch }))

  async function submit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    const n = (v: string) => (v === '' ? null : Number(v))
    const d = (v: string) => (v === '' ? null : v)
    const body = {
      ...f,
      email: d(f.email),
      dateContacted: d(f.dateContacted),
      proposalDate: d(f.proposalDate),
      accepted: f.accepted || null,
      acceptedDate: d(f.acceptedDate),
      conductedFrom: d(f.conductedFrom),
      conductedTo: d(f.conductedTo),
      budgetRequired: n(f.budgetRequired),
      feeCharged: n(f.feeCharged),
    }
    try {
      if (client) await api.put(`/training-clients/${client.id}`, body)
      else await api.post(`/training-programs/${programId}/clients`, body)
      onDone()
    } catch (err) {
      setError((err as Error).message)
    }
  }

  // each step only opens once the one before it is done
  const canAnswer = f.proposalSubmitted
  const canConduct = f.accepted === 'YES'

  return (
    <form className="card form" onSubmit={submit}>
      <h2>{client ? `Edit ${client.clientName}` : 'New client'}</h2>
      <ErrorBox message={error} />
      <div className="form-grid">
        <label className="wide">
          Client name
          <input value={f.clientName} onChange={(e) => set({ clientName: e.target.value })} required />
        </label>
        <label>
          Contact person
          <input value={f.contactPerson} onChange={(e) => set({ contactPerson: e.target.value })} />
        </label>
        <label>
          Phone
          <input value={f.phone} onChange={(e) => set({ phone: e.target.value })} />
        </label>
        <label>
          Email
          <input type="email" value={f.email} onChange={(e) => set({ email: e.target.value })} />
        </label>
        <label>
          Date contacted
          <input type="date" value={f.dateContacted} onChange={(e) => set({ dateContacted: e.target.value })} />
        </label>
        <label>
          Budget required (₹)
          <input type="number" min={0} value={f.budgetRequired} onChange={(e) => set({ budgetRequired: e.target.value })} />
        </label>
        <label>
          Fee charged (₹)
          <input type="number" min={0} value={f.feeCharged} onChange={(e) => set({ feeCharged: e.target.value })} />
        </label>
      </div>

      <div className="steps">
        <fieldset>
          <legend>1. Proposal</legend>
          <label className="checkbox">
            <input
              type="checkbox"
              checked={f.proposalSubmitted}
              onChange={(e) =>
                set(e.target.checked ? { proposalSubmitted: true } : { proposalSubmitted: false, accepted: '', conducted: false })
              }
            />
            Proposal submitted
          </label>
          {f.proposalSubmitted && (
            <label>
              Date
              <input type="date" value={f.proposalDate} onChange={(e) => set({ proposalDate: e.target.value })} required />
            </label>
          )}
        </fieldset>

        <fieldset disabled={!canAnswer}>
          <legend>2. Client's answer</legend>
          <label>
            Accepted
            <select
              value={f.accepted}
              onChange={(e) => {
                const v = e.target.value as '' | Acceptance
                set(v === 'YES' ? { accepted: v } : { accepted: v, conducted: false })
              }}
            >
              <option value="">No answer yet</option>
              <option value="YES">Yes</option>
              <option value="NEEDS_REVISION">Needs revision</option>
              <option value="NO">No</option>
            </select>
          </label>
          {f.accepted && (
            <>
              <label>
                Date
                <input type="date" value={f.acceptedDate} onChange={(e) => set({ acceptedDate: e.target.value })} required />
              </label>
              <label>
                Remarks
                <input value={f.acceptedRemarks} onChange={(e) => set({ acceptedRemarks: e.target.value })} />
              </label>
            </>
          )}
          {!canAnswer && <p className="muted small">Submit the proposal first.</p>}
          {f.accepted === 'YES' && !client?.budgetId && (
            <p className="muted small">Saving creates a training budget for the budget required amount.</p>
          )}
        </fieldset>

        <fieldset disabled={!canConduct}>
          <legend>3. Delivery</legend>
          <label className="checkbox">
            <input type="checkbox" checked={f.conducted} onChange={(e) => set({ conducted: e.target.checked })} />
            Conducted
          </label>
          {f.conducted && (
            <>
              <label>
                From
                <input type="date" value={f.conductedFrom} onChange={(e) => set({ conductedFrom: e.target.value })} required />
              </label>
              <label>
                To
                <input type="date" value={f.conductedTo} onChange={(e) => set({ conductedTo: e.target.value })} />
              </label>
            </>
          )}
          {!canConduct && <p className="muted small">Only once the client has accepted.</p>}
        </fieldset>
      </div>

      <div className="form-actions">
        <button className="btn primary" type="submit">Save</button>
        <button className="btn" type="button" onClick={onCancel}>Cancel</button>
      </div>
    </form>
  )
}
