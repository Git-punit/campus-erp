import { Link } from 'react-router'
import CrudSection, { enumOptions, str, type CrudConfig } from '../components/CrudSection'
import { Badge, Money, UsageBar } from '../components/ui'
import { date } from '../lib/format'
import type { CampusEvent } from '../lib/types'

const STATUSES = ['PLANNED', 'ONGOING', 'COMPLETED', 'CANCELLED']

export default function EventsPage() {
  const config: CrudConfig<CampusEvent> = {
    noun: 'event',
    listPath: '/events',
    savePath: '/events',
    columns: [
      { label: 'Event', render: (e) => <strong>{e.name}</strong> },
      { label: 'Type', render: (e) => e.type.charAt(0) + e.type.slice(1).toLowerCase() },
      { label: 'Dates', render: (e) => (e.startDate === e.endDate ? date(e.startDate) : `${date(e.startDate)} to ${date(e.endDate)}`) },
      { label: 'Venue', render: (e) => e.venue ?? '-' },
      { label: 'Status', render: (e) => <Badge value={e.status} /> },
      {
        label: 'Budget',
        render: (e) =>
          e.budget ? (
            <Link to={`/budgets/${e.budget.id}`} className="budget-cell">
              <Money value={e.budget.utilized} /> of <Money value={e.budget.allocated} />
              <UsageBar percent={e.budget.percentUsed} />
            </Link>
          ) : (
            <span className="muted">none</span>
          ),
      },
    ],
    fields: [
      { name: 'name', label: 'Name', required: true, wide: true },
      { name: 'type', label: 'Type', required: true, placeholder: 'Workshop, festival...' },
      { name: 'startDate', label: 'Starts', type: 'date', required: true },
      { name: 'endDate', label: 'Ends', type: 'date', required: true },
      { name: 'venue', label: 'Venue' },
      { name: 'status', label: 'Status', type: 'select', required: true, options: enumOptions(STATUSES) },
      { name: 'budgetAmount', label: 'Budget (₹, leave empty for none)', type: 'number' },
    ],
    empty: { name: '', type: '', startDate: '', endDate: '', venue: '', status: 'PLANNED', budgetAmount: '' },
    toForm: (e) => ({
      name: e.name,
      type: e.type,
      startDate: e.startDate,
      endDate: e.endDate,
      venue: str(e.venue),
      status: e.status,
      budgetAmount: str(e.budget?.allocated),
    }),
  }
  return (
    <>
      <h1>Events</h1>
      <CrudSection config={config} />
    </>
  )
}
