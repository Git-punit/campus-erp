import { useState } from 'react'
import { Link } from 'react-router'
import { api } from '../api'
import { useAuth } from '../auth'
import { date, label, money } from '../lib/format'
import type { BudgetSummary, Expense } from '../lib/types'
import { useLoad } from '../lib/useLoad'
import { Badge, Empty, ErrorBox, Loading, Money } from './ui'

/**
 * Expense list with approve / reject for admins and delete for pending ones.
 * When the budget is known, approving warns before going over the allocation.
 */
export default function ExpenseTable({
  path,
  showBudget,
  budget,
  onChange,
}: {
  path: string
  showBudget?: boolean
  budget?: BudgetSummary
  onChange?: () => void
}) {
  const { isAdmin, canEdit } = useAuth()
  const list = useLoad<Expense[]>(path)
  const [error, setError] = useState<string | null>(null)

  function changed() {
    list.reload()
    onChange?.()
  }

  async function approve(e: Expense) {
    setError(null)
    const remark = prompt(`Approve ${money(e.amount)} for "${e.description}"?\nOptional remark:`, '')
    if (remark === null) return
    try {
      await api.post(`/expenses/${e.id}/approve`, { remark, allowOverspend: false })
      changed()
    } catch (err) {
      const msg = (err as Error).message
      // the server refuses overspending unless we confirm it
      if (msg.includes('over its allocation')) {
        if (!confirm(msg.replace(' Approve again with allowOverspend to go ahead.', '') + '\n\nApprove anyway?')) return
        try {
          await api.post(`/expenses/${e.id}/approve`, { remark, allowOverspend: true })
          changed()
        } catch (err2) {
          setError((err2 as Error).message)
        }
      } else {
        setError(msg)
      }
    }
  }

  async function reject(e: Expense) {
    setError(null)
    const remark = prompt(`Reject ${money(e.amount)} for "${e.description}"?\nReason (required):`, '')
    if (remark === null) return
    try {
      await api.post(`/expenses/${e.id}/reject`, { remark })
      changed()
    } catch (err) {
      setError((err as Error).message)
    }
  }

  async function remove(e: Expense) {
    if (!confirm('Delete this pending request?')) return
    try {
      await api.delete(`/expenses/${e.id}`)
      changed()
    } catch (err) {
      setError((err as Error).message)
    }
  }

  if (!list.data) return list.error ? <ErrorBox message={list.error} /> : <Loading />

  return (
    <>
      <ErrorBox message={error ?? list.error} />
      {list.data.length === 0 ? (
        <Empty>No expenses here.</Empty>
      ) : (
        <table className="table">
          <thead>
            <tr>
              <th>Date</th>
              {showBudget && <th>Budget</th>}
              <th>Description</th>
              <th>Category</th>
              <th className="num">Amount</th>
              <th>Status</th>
              <th>Raised by</th>
              <th>Decision</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {list.data.map((e) => {
              const wouldOverspend = budget && e.status === 'PENDING' && budget.utilized + e.amount > budget.allocated
              return (
                <tr key={e.id}>
                  <td>{date(e.date)}</td>
                  {showBudget && (
                    <td className="small">
                      <Link to={`/budgets/${e.budgetId}`}>{e.budgetName}</Link>
                    </td>
                  )}
                  <td>{e.description}</td>
                  <td>{label(e.category)}</td>
                  <td className="num">
                    <Money value={e.amount} />
                  </td>
                  <td>
                    <Badge value={e.status} />
                    {wouldOverspend && <div className="flag near small">would overspend</div>}
                  </td>
                  <td>{e.raisedBy}</td>
                  <td className="small">
                    {e.decidedBy ? (
                      <>
                        {e.decidedBy}
                        {e.decisionRemark ? `: ${e.decisionRemark}` : ''}
                      </>
                    ) : (
                      '-'
                    )}
                  </td>
                  <td className="actions">
                    {e.status === 'PENDING' && isAdmin && (
                      <>
                        <button className="link-btn" onClick={() => approve(e)}>
                          Approve
                        </button>
                        <button className="link-btn danger" onClick={() => reject(e)}>
                          Reject
                        </button>
                      </>
                    )}
                    {e.status === 'PENDING' && canEdit && !isAdmin && (
                      <button className="link-btn danger" onClick={() => remove(e)}>
                        Delete
                      </button>
                    )}
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      )}
    </>
  )
}
