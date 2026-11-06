import { useEffect, useState, type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router'
import { api, ApiError } from '../api'
import { useAuth } from '../auth'
import { Badge, Empty, ErrorBox, Loading, Money, UsageBar } from '../components/ui'
import { date, label } from '../lib/format'
import type { Faculty, MemberRole, Project, ProjectStatus } from '../lib/types'
import { useLoad } from '../lib/useLoad'

const STATUSES: ProjectStatus[] = ['ONGOING', 'COMPLETED', 'CLOSED']

export default function ProjectsPage() {
  const { canEdit } = useAuth()
  const [params] = useSearchParams()
  const open = Number(params.get('open'))
  const list = useLoad<Project[]>('/projects')
  const [editing, setEditing] = useState<Project | 'new' | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (open && list.data) document.getElementById(`project-${open}`)?.scrollIntoView({ block: 'center' })
  }, [open, list.data])

  async function remove(p: Project) {
    if (!confirm(`Delete "${p.title}"?`)) return
    setError(null)
    try {
      await api.delete(`/projects/${p.id}`)
      list.reload()
    } catch (e) {
      setError((e as Error).message)
    }
  }

  return (
    <>
      <div className="page-head">
        <h1>Research projects</h1>
        {canEdit && (
          <button className="btn primary" onClick={() => setEditing('new')}>
            Add project
          </button>
        )}
      </div>
      <p className="muted small">
        Utilized is the total of approved expenses on the project's budget. Pending requests are shown separately.
      </p>
      <ErrorBox message={error ?? list.error} />
      {editing && (
        <ProjectForm
          project={editing === 'new' ? null : editing}
          onDone={() => {
            setEditing(null)
            list.reload()
          }}
          onCancel={() => setEditing(null)}
        />
      )}
      {!list.data ? (
        <Loading />
      ) : list.data.length === 0 ? (
        <Empty>No projects yet.</Empty>
      ) : (
        <table className="table">
          <thead>
            <tr>
              <th>Project</th>
              <th>Team</th>
              <th>Agency</th>
              <th className="num">Sanctioned</th>
              <th className="num">Utilized</th>
              <th className="num">Balance</th>
              <th>Used</th>
              <th>Status</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {list.data.map((p) => (
              <tr key={p.id} id={`project-${p.id}`} className={p.id === open ? 'highlight' : undefined}>
                <td className="title-cell">
                  <strong>{p.title}</strong>
                  <div className="muted small">
                    Sanctioned {date(p.sanctionDate)}
                    {p.endDate ? `, ends ${date(p.endDate)}` : ''}
                  </div>
                </td>
                <td className="small">
                  {p.team.map((m) => (
                    <div key={m.facultyId}>
                      <Link to={`/faculty/${m.facultyId}?tab=projects`}>{m.name}</Link>
                      {m.role === 'PI' && <span className="badge tone-pi">PI</span>}
                    </div>
                  ))}
                </td>
                <td>{p.fundingAgency}</td>
                <td className="num"><Money value={p.sanctionedAmount} /></td>
                <td className="num">
                  <Money value={p.utilized} />
                  {p.pending > 0 && <div className="muted small">+ <Money value={p.pending} /> pending</div>}
                </td>
                <td className="num"><Money value={p.balance} /></td>
                <td><UsageBar percent={p.percentUsed} /></td>
                <td><Badge value={p.status} /></td>
                <td className="actions">
                  {p.budgetId && (
                    <Link className="link-btn" to={`/budgets/${p.budgetId}`}>
                      Expenses
                    </Link>
                  )}
                  {canEdit && (
                    <>
                      <button className="link-btn" onClick={() => setEditing(p)}>
                        Edit
                      </button>
                      <button className="link-btn danger" onClick={() => remove(p)}>
                        Delete
                      </button>
                    </>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </>
  )
}

interface Member {
  facultyId: string
  role: MemberRole
}

function ProjectForm({ project, onDone, onCancel }: { project: Project | null; onDone: () => void; onCancel: () => void }) {
  const faculty = useLoad<Faculty[]>('/faculty')
  const [form, setForm] = useState({
    title: project?.title ?? '',
    fundingAgency: project?.fundingAgency ?? '',
    sanctionedAmount: project ? String(project.sanctionedAmount) : '',
    sanctionDate: project?.sanctionDate ?? '',
    startDate: project?.startDate ?? '',
    endDate: project?.endDate ?? '',
    status: project?.status ?? 'ONGOING',
  })
  const [team, setTeam] = useState<Member[]>(
    project ? project.team.map((m) => ({ facultyId: String(m.facultyId), role: m.role })) : [{ facultyId: '', role: 'PI' }],
  )
  const [error, setError] = useState<string | null>(null)

  async function submit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    try {
      const body = {
        ...form,
        sanctionedAmount: Number(form.sanctionedAmount),
        startDate: form.startDate || null,
        endDate: form.endDate || null,
        team: team.filter((m) => m.facultyId).map((m) => ({ facultyId: Number(m.facultyId), role: m.role })),
      }
      if (project) await api.put(`/projects/${project.id}`, body)
      else await api.post('/projects', body)
      onDone()
    } catch (err) {
      const fe = err instanceof ApiError ? Object.values(err.fieldErrors) : []
      setError([(err as Error).message, ...fe].join('. '))
    }
  }

  return (
    <form className="card form" onSubmit={submit}>
      <h2>{project ? 'Edit project' : 'New research project'}</h2>
      {!project && <p className="muted small">A budget for the sanctioned amount is created with the project.</p>}
      <ErrorBox message={error} />
      <div className="form-grid">
        <label className="wide">
          Title
          <input value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} required />
        </label>
        <label>
          Funding agency
          <input value={form.fundingAgency} onChange={(e) => setForm({ ...form, fundingAgency: e.target.value })} placeholder="DST, SERB, AICTE..." required />
        </label>
        <label>
          Sanctioned amount (₹)
          <input type="number" min={0} value={form.sanctionedAmount} onChange={(e) => setForm({ ...form, sanctionedAmount: e.target.value })} required />
        </label>
        <label>
          Sanction date
          <input type="date" value={form.sanctionDate} onChange={(e) => setForm({ ...form, sanctionDate: e.target.value })} required />
        </label>
        <label>
          Start date
          <input type="date" value={form.startDate} onChange={(e) => setForm({ ...form, startDate: e.target.value })} />
        </label>
        <label>
          End date
          <input type="date" value={form.endDate} onChange={(e) => setForm({ ...form, endDate: e.target.value })} />
        </label>
        <label>
          Status
          <select value={form.status} onChange={(e) => setForm({ ...form, status: e.target.value as ProjectStatus })}>
            {STATUSES.map((s) => (
              <option key={s} value={s}>
                {label(s)}
              </option>
            ))}
          </select>
        </label>
      </div>
      <h3>Project team</h3>
      {team.map((m, i) => (
        <div className="row-edit" key={i}>
          <select value={m.facultyId} onChange={(e) => setTeam(team.map((x, j) => (j === i ? { ...x, facultyId: e.target.value } : x)))}>
            <option value="">Choose teacher...</option>
            {faculty.data?.map((f) => (
              <option key={f.id} value={f.id}>
                {f.name} ({f.department.code})
              </option>
            ))}
          </select>
          <select value={m.role} onChange={(e) => setTeam(team.map((x, j) => (j === i ? { ...x, role: e.target.value as MemberRole } : x)))}>
            <option value="PI">Principal Investigator</option>
            <option value="CO_INVESTIGATOR">Co-investigator</option>
          </select>
          <button type="button" className="link-btn danger" onClick={() => setTeam(team.filter((_, j) => j !== i))}>
            Remove
          </button>
        </div>
      ))}
      <button type="button" className="link-btn" onClick={() => setTeam([...team, { facultyId: '', role: 'CO_INVESTIGATOR' }])}>
        + Add co-investigator
      </button>
      <div className="form-actions">
        <button className="btn primary" type="submit">Save</button>
        <button className="btn" type="button" onClick={onCancel}>Cancel</button>
      </div>
    </form>
  )
}
