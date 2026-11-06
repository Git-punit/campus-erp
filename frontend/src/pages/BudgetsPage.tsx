import { useState, type FormEvent } from 'react'
import { Link } from 'react-router'
import { api, ApiError } from '../api'
import { useAuth } from '../auth'
import ExpenseTable from '../components/ExpenseTable'
import { Badge, Empty, ErrorBox, Loading, Money, Tabs, UsageBar } from '../components/ui'
import { label } from '../lib/format'
import type { BudgetSummary, BudgetType, Department, Expense } from '../lib/types'
import { useLoad } from '../lib/useLoad'

const TYPES: BudgetType[] = ['DEPARTMENT', 'EVENT', 'TRAINING', 'PROJECT']

export default function BudgetsPage() {
  const { isAdmin } = useAuth()
  const pending = useLoad<Expense[]>('/expenses?status=PENDING')
  const count = pending.data?.length ?? 0
  return (
    <>
      <h1>Budgets</h1>
      <Tabs
        key={pending.data ? 'ready' : 'loading'}
        initial={isAdmin && count > 0 ? 'pending' : 'budgets'}
        tabs={[
          { key: 'budgets', label: 'Budgets', content: () => <BudgetList /> },
          {
            key: 'pending',
            label: `Waiting for approval${count ? ` (${count})` : ''}`,
            content: () => <ExpenseTable path="/expenses?status=PENDING" showBudget onChange={pending.reload} />,
          },
          { key: 'all', label: 'All expenses', content: () => <ExpenseTable path="/expenses" showBudget /> },
        ]}
      />
    </>
  )
}

function currentFinancialYear() {
  const d = new Date()
  const start = d.getMonth() >= 3 ? d.getFullYear() : d.getFullYear() - 1
  return `${start}-${String((start + 1) % 100).padStart(2, '0')}`
}

function BudgetList() {
  const { canEdit } = useAuth()
  const departments = useLoad<Department[]>('/departments')
  const [type, setType] = useState('')
  const [departmentId, setDepartmentId] = useState('')
  const [fy, setFy] = useState('')
  const [adding, setAdding] = useState(false)

  const q = new URLSearchParams()
  if (type) q.set('type', type)
  if (departmentId) q.set('departmentId', departmentId)
  if (fy) q.set('financialYear', fy)
  const list = useLoad<BudgetSummary[]>(`/budgets?${q}`)

  const years = Array.from(new Set((list.data ?? []).map((b) => b.financialYear).concat(currentFinancialYear()))).sort().reverse()

  return (
    <>
      <div className="section-head">
        <div className="filters">
          <label>
            Type
            <select value={type} onChange={(e) => setType(e.target.value)}>
              <option value="">All</option>
              {TYPES.map((t) => (
                <option key={t} value={t}>
                  {label(t)}
                </option>
              ))}
            </select>
          </label>
          <label>
            Department
            <select value={departmentId} onChange={(e) => setDepartmentId(e.target.value)}>
              <option value="">All</option>
              {departments.data?.map((d) => (
                <option key={d.id} value={d.id}>
                  {d.code}
                </option>
              ))}
            </select>
          </label>
          <label>
            Financial year
            <select value={fy} onChange={(e) => setFy(e.target.value)}>
              <option value="">All</option>
              {years.map((y) => (
                <option key={y}>{y}</option>
              ))}
            </select>
          </label>
        </div>
        <span className="spacer" />
        {canEdit && (
          <button className="btn primary" onClick={() => setAdding(true)}>
            Add department budget
          </button>
        )}
      </div>
      <p className="muted small">
        Event, training and project budgets are created from their own records. Only approved expenses count as used.
      </p>
      {adding && (
        <DepartmentBudgetForm
          departments={departments.data ?? []}
          onDone={() => {
            setAdding(false)
            list.reload()
          }}
          onCancel={() => setAdding(false)}
        />
      )}
      <ErrorBox message={list.error} />
      {!list.data ? (
        <Loading />
      ) : list.data.length === 0 ? (
        <Empty>No budgets match these filters.</Empty>
      ) : (
        <BudgetTable rows={list.data} />
      )}
    </>
  )
}

export function BudgetTable({ rows }: { rows: BudgetSummary[] }) {
  return (
    <table className="table">
      <thead>
        <tr>
          <th>Budget</th>
          <th>Type</th>
          <th>FY</th>
          <th className="num">Allocated</th>
          <th className="num">Utilized</th>
          <th className="num">Pending</th>
          <th className="num">Balance</th>
          <th>Used</th>
          <th></th>
        </tr>
      </thead>
      <tbody>
        {rows.map((b) => (
          <tr key={b.id}>
            <td>
              <Link to={`/budgets/${b.id}`}>{b.name}</Link>
            </td>
            <td>
              <Badge value={b.type} tone="tone-type" />
            </td>
            <td>{b.financialYear}</td>
            <td className="num"><Money value={b.allocated} /></td>
            <td className="num"><Money value={b.utilized} /></td>
            <td className="num muted"><Money value={b.pending} /></td>
            <td className="num"><Money value={b.balance} /></td>
            <td><UsageBar percent={b.percentUsed} /></td>
            <td>
              {b.overspent ? (
                <span className="flag over">Overspent</span>
              ) : b.pendingWouldOverspend ? (
                <span className="flag near" title="Approving all pending requests would go over">Pending would overspend</span>
              ) : null}
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}

function DepartmentBudgetForm({
  departments,
  onDone,
  onCancel,
}: {
  departments: Department[]
  onDone: () => void
  onCancel: () => void
}) {
  const [departmentId, setDepartmentId] = useState('')
  const [fy, setFy] = useState(currentFinancialYear())
  const [allocated, setAllocated] = useState('')
  const [error, setError] = useState<string | null>(null)

  async function submit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    try {
      await api.post('/budgets', { departmentId: Number(departmentId), financialYear: fy, allocated: Number(allocated) })
      onDone()
    } catch (err) {
      const extra = err instanceof ApiError ? Object.values(err.fieldErrors).join('. ') : ''
      setError([(err as Error).message, extra].filter(Boolean).join('. '))
    }
  }

  return (
    <form className="card form" onSubmit={submit}>
      <h2>New department budget</h2>
      <ErrorBox message={error} />
      <div className="form-grid">
        <label>
          Department
          <select value={departmentId} onChange={(e) => setDepartmentId(e.target.value)} required>
            <option value="">Choose...</option>
            {departments.map((d) => (
              <option key={d.id} value={d.id}>
                {d.code} - {d.name}
              </option>
            ))}
          </select>
        </label>
        <label>
          Financial year (April to March)
          <input value={fy} onChange={(e) => setFy(e.target.value)} placeholder="2026-27" required />
        </label>
        <label>
          Allocated amount (₹)
          <input type="number" min={0} value={allocated} onChange={(e) => setAllocated(e.target.value)} required />
        </label>
      </div>
      <div className="form-actions">
        <button className="btn primary" type="submit">
          Save
        </button>
        <button className="btn" type="button" onClick={onCancel}>
          Cancel
        </button>
      </div>
    </form>
  )
}
