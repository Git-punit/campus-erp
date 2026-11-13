import { useState, type FormEvent } from 'react'
import { Link, useParams } from 'react-router'
import { api, ApiError } from '../api'
import { useAuth } from '../auth'
import ExpenseTable from '../components/ExpenseTable'
import { Badge, ErrorBox, Loading, Money, UsageBar } from '../components/ui'
import { todayIso } from '../lib/format'
import type { BudgetSummary, ExpenseCategory } from '../lib/types'
import { useLoad } from '../lib/useLoad'

const CATEGORIES: ExpenseCategory[] = ['EQUIPMENT', 'TRAVEL', 'CONSUMABLES', 'HONORARIUM', 'OTHER']

export default function BudgetDetailPage() {
  const { id } = useParams()
  const { canEdit } = useAuth()
  const budget = useLoad<BudgetSummary>(`/budgets/${id}`)
  const [version, setVersion] = useState(0)
  const [raising, setRaising] = useState(false)
  const [editingAmount, setEditingAmount] = useState(false)

  if (budget.error) return <ErrorBox message={budget.error} />
  if (!budget.data) return <Loading />
  const b = budget.data

  const refresh = () => {
    budget.reload()
    setVersion((v) => v + 1)
  }

  const sourceLink =
    b.source?.kind === 'PROJECT' ? `/projects?open=${b.source.id}` : b.source?.kind === 'TRAINING' ? `/training/${b.source.id}` : b.source?.kind === 'EVENT' ? '/events' : null

  return (
    <>
      <h1>{b.name}</h1>
      <div className="card money-summary">
        <div>
          <span>Type</span>
          <Badge value={b.type} tone="tone-type" />
        </div>
        <div>
          <span>Financial year</span>
          {b.financialYear}
        </div>
        <div>
          <span>Allocated</span>
          <strong><Money value={b.allocated} /></strong>
          {canEdit && b.type === 'DEPARTMENT' && !editingAmount && (
            <button className="link-btn small" onClick={() => setEditingAmount(true)}>
              change
            </button>
          )}
        </div>
        <div>
          <span>Utilized</span>
          <strong><Money value={b.utilized} /></strong>
        </div>
        <div>
          <span>Pending</span>
          <Money value={b.pending} />
        </div>
        <div>
          <span>Balance</span>
          <strong><Money value={b.balance} /></strong>
        </div>
        <div className="bar-cell">
          <UsageBar percent={b.percentUsed} />
          {b.overspent && <span className="flag over">Overspent</span>}
          {!b.overspent && b.pendingWouldOverspend && <span className="flag near">Pending would overspend</span>}
        </div>
        {b.source && sourceLink && (
          <div>
            <span>For</span>
            <Link to={sourceLink}>{b.source.name}</Link>
          </div>
        )}
      </div>

      {editingAmount && <AllocationForm budget={b} onDone={() => { setEditingAmount(false); refresh() }} onCancel={() => setEditingAmount(false)} />}

      <div className="section-head">
        <h2>Expenses</h2>
        <span className="spacer" />
        {canEdit && (
          <button className="btn primary" onClick={() => setRaising(true)}>
            Raise expense request
          </button>
        )}
      </div>
      {raising && <ExpenseForm budgetId={b.id} onDone={() => { setRaising(false); refresh() }} onCancel={() => setRaising(false)} />}
      <ExpenseTable key={version} path={`/expenses?budgetId=${b.id}`} budget={b} onChange={refresh} />
    </>
  )
}

function AllocationForm({ budget, onDone, onCancel }: { budget: BudgetSummary; onDone: () => void; onCancel: () => void }) {
  const [amount, setAmount] = useState(String(budget.allocated))
  const [error, setError] = useState<string | null>(null)
  async function submit(e: FormEvent) {
    e.preventDefault()
    try {
      await api.put(`/budgets/${budget.id}`, { allocated: Number(amount) })
      onDone()
    } catch (err) {
      setError((err as Error).message)
    }
  }
  return (
    <form className="card form inline-form" onSubmit={submit}>
      <ErrorBox message={error} />
      <label>
        New allocated amount (₹)
        <input type="number" min={0} value={amount} onChange={(e) => setAmount(e.target.value)} required />
      </label>
      <button className="btn primary" type="submit">Save</button>
      <button className="btn" type="button" onClick={onCancel}>Cancel</button>
    </form>
  )
}

function ExpenseForm({ budgetId, onDone, onCancel }: { budgetId: number; onDone: () => void; onCancel: () => void }) {
  const [form, setForm] = useState({ date: todayIso(), amount: '', category: 'EQUIPMENT', description: '' })
  const [receipt, setReceipt] = useState<File | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})

  async function submit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setFieldErrors({})
    try {
      const created = await api.post<{ id: number }>('/expenses', { budgetId, ...form, amount: Number(form.amount) })
      if (receipt) {
        try {
          await api.upload(`/expenses/${created.id}/receipt`, receipt)
        } catch (err) {
          // the request itself is saved, so close the form (no duplicate on retry) and say so
          alert(`The request was saved, but the receipt wasn't: ${(err as Error).message}\nAttach it from the list.`)
        }
      }
      onDone()
    } catch (err) {
      setError((err as Error).message)
      if (err instanceof ApiError) setFieldErrors(err.fieldErrors)
    }
  }

  return (
    <form className="card form" onSubmit={submit}>
      <h2>New expense request</h2>
      <p className="muted small">It stays pending until an admin approves or rejects it.</p>
      <ErrorBox message={error} />
      <div className="form-grid">
        <label>
          Date
          <input type="date" value={form.date} onChange={(e) => setForm({ ...form, date: e.target.value })} required />
        </label>
        <label>
          Amount (₹)
          <input type="number" min={1} step="any" value={form.amount} onChange={(e) => setForm({ ...form, amount: e.target.value })} required />
          {fieldErrors.amount && <span className="field-error">{fieldErrors.amount}</span>}
        </label>
        <label>
          Category
          <select value={form.category} onChange={(e) => setForm({ ...form, category: e.target.value })}>
            {CATEGORIES.map((c) => (
              <option key={c} value={c}>
                {c.charAt(0) + c.slice(1).toLowerCase()}
              </option>
            ))}
          </select>
        </label>
        <label className="wide">
          Description
          <input value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} required />
        </label>
        <label className="wide">
          Receipt (optional: PDF, JPG or PNG, up to 5 MB)
          <input
            type="file"
            accept="application/pdf,image/jpeg,image/png"
            onChange={(e) => setReceipt(e.target.files?.[0] ?? null)}
          />
        </label>
      </div>
      <div className="form-actions">
        <button className="btn primary" type="submit">Submit request</button>
        <button className="btn" type="button" onClick={onCancel}>Cancel</button>
      </div>
    </form>
  )
}
