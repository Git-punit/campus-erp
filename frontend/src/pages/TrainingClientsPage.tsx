import { Link, useSearchParams } from 'react-router'
import { Badge, Empty, ErrorBox, Loading, Money } from '../components/ui'
import { date } from '../lib/format'
import type { TrainingClient } from '../lib/types'
import { useLoad } from '../lib/useLoad'

// the four numbers of the dashboard's training pipeline, in order
const STAGES = [
  { key: 'reached', label: 'Reached', note: 'Every client a program was pitched to' },
  { key: 'proposals', label: 'Proposals', note: 'Clients who were sent a proposal' },
  { key: 'accepted', label: 'Accepted', note: 'Clients who said yes' },
  { key: 'conducted', label: 'Conducted', note: 'Clients whose training has been held' },
]

/** The clients behind one number of the dashboard's training pipeline, across all programs. */
export default function TrainingClientsPage() {
  const [params] = useSearchParams()
  const stage = STAGES.find((s) => s.key === params.get('stage')) ?? STAGES[0]
  const clients = useLoad<TrainingClient[]>(`/training-clients?stage=${stage.key}`)

  return (
    <>
      <h1>Training clients</h1>
      <nav className="tabs" aria-label="Pipeline step">
        {STAGES.map((s) => (
          <Link key={s.key} to={`?stage=${s.key}`} className={s === stage ? 'tab active' : 'tab'}
            aria-current={s === stage ? 'page' : undefined}>
            {s.label}
          </Link>
        ))}
      </nav>
      <p className="muted small">
        {stage.note}{clients.data ? `: ${clients.data.length}` : ''}. Click a program to update a client.
      </p>
      <ErrorBox message={clients.error} />
      {clients.loading ? (
        <Loading />
      ) : clients.data?.length === 0 ? (
        <Empty>No clients at this step yet.</Empty>
      ) : (
        <table className="table">
          <thead>
            <tr>
              <th>Client</th>
              <th>Program</th>
              <th>Contacted</th>
              <th>Proposal</th>
              <th>Accepted</th>
              <th>Conducted</th>
              <th className="num">Fee</th>
              <th className="num">Profit / loss</th>
            </tr>
          </thead>
          <tbody>
            {clients.data?.map((c) => (
              <tr key={c.id}>
                <td>
                  <strong>{c.clientName}</strong>
                  <div className="muted small">{[c.contactPerson, c.phone, c.email].filter(Boolean).join(', ')}</div>
                </td>
                <td>
                  <Link to={`/training/${c.programId}`}>{c.programName}</Link>
                </td>
                <td>{date(c.dateContacted)}</td>
                <td>{c.proposalSubmitted ? date(c.proposalDate) : 'No'}</td>
                <td>{c.accepted ? <Badge value={c.accepted} tone={'tone-acc-' + c.accepted.toLowerCase()} /> : '-'}</td>
                <td>{c.conducted ? `${date(c.conductedFrom)} to ${date(c.conductedTo)}` : 'No'}</td>
                <td className="num"><Money value={c.feeCharged} /></td>
                <td className="num"><Money value={c.profit} /></td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </>
  )
}
