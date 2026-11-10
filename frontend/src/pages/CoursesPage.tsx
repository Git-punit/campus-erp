import { useState, type FormEvent } from 'react'
import { api, ApiError } from '../api'
import { useAuth } from '../auth'
import CrudSection, { optionsOf, str, type CrudConfig } from '../components/CrudSection'
import { Empty, ErrorBox, Loading, Tabs, WarningBox } from '../components/ui'
import { date, day, label, time } from '../lib/format'
import type {
  AcademicYear,
  ClassChange,
  Course,
  Day,
  Department,
  Faculty,
  Offering,
  Section,
  Slot,
  TeachingRole,
} from '../lib/types'
import { useLoad } from '../lib/useLoad'

const DAYS: Day[] = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY']
const ROLES: TeachingRole[] = ['THEORY', 'LAB', 'TUTORIAL']

export default function CoursesPage() {
  return (
    <>
      <h1>Courses</h1>
      <Tabs
        tabs={[
          { key: 'offerings', label: 'Course offerings', content: () => <Offerings /> },
          { key: 'catalogue', label: 'Catalogue', content: () => <Catalogue /> },
        ]}
      />
    </>
  )
}

function Catalogue() {
  const departments = useLoad<Department[]>('/departments')
  const config: CrudConfig<Course> = {
    noun: 'course',
    listPath: '/courses',
    savePath: '/courses',
    columns: [
      { label: 'Code', render: (c) => c.code },
      { label: 'Name', render: (c) => c.name },
      { label: 'Credits', className: 'num', render: (c) => c.credits },
      { label: 'Department', render: (c) => c.department.code },
    ],
    fields: [
      { name: 'code', label: 'Code', required: true },
      { name: 'name', label: 'Name', required: true, wide: true },
      { name: 'credits', label: 'Credits', type: 'number', required: true },
      { name: 'departmentId', label: 'Department', type: 'select', required: true, options: optionsOf(departments.data, (d) => d.code + ' - ' + d.name) },
    ],
    empty: { code: '', name: '', credits: '4', departmentId: '' },
    toForm: (c) => ({ code: c.code, name: c.name, credits: str(c.credits), departmentId: str(c.department.id) }),
    csv: {
      fileName: 'courses',
      row: (c) => ({ code: c.code, name: c.name, credits: c.credits, department: c.department.code }),
    },
  }
  return <CrudSection config={config} />
}

// ---------------------------------------------------------------- offerings

function Offerings() {
  const { canEdit } = useAuth()
  const years = useLoad<AcademicYear[]>('/academic-years')
  const sections = useLoad<Section[]>('/sections')
  const [year, setYear] = useState<string | null>(null)
  const [semester, setSemester] = useState('')
  const [sectionId, setSectionId] = useState('')
  const chosenYear = year ?? years.data?.find((y) => y.current)?.name ?? ''

  const query = new URLSearchParams()
  if (chosenYear) query.set('year', chosenYear)
  if (semester) query.set('semester', semester)
  if (sectionId) query.set('sectionId', sectionId)
  const list = useLoad<Offering[]>(years.data ? `/offerings?${query}` : null)

  const [editing, setEditing] = useState<Offering | 'new' | null>(null)
  const [changesFor, setChangesFor] = useState<Offering | null>(null)
  const [warnings, setWarnings] = useState<string[]>([])
  const [error, setError] = useState<string | null>(null)

  async function remove(o: Offering) {
    if (!confirm(`Delete ${o.course.code} for Sem ${o.semester} ${o.section.code}?`)) return
    setError(null)
    try {
      await api.delete(`/offerings/${o.id}`)
      list.reload()
    } catch (e) {
      setError((e as Error).message)
    }
  }

  return (
    <>
      <div className="section-head">
        <div className="filters">
          <label>
            Academic year
            <select value={chosenYear} onChange={(e) => setYear(e.target.value)}>
              <option value="">All</option>
              {years.data?.map((y) => (
                <option key={y.id} value={y.name}>
                  {y.name}
                </option>
              ))}
            </select>
          </label>
          <label>
            Semester
            <select value={semester} onChange={(e) => setSemester(e.target.value)}>
              <option value="">All</option>
              {[1, 2, 3, 4, 5, 6, 7, 8].map((n) => (
                <option key={n}>{n}</option>
              ))}
            </select>
          </label>
          <label>
            Section
            <select value={sectionId} onChange={(e) => setSectionId(e.target.value)}>
              <option value="">All</option>
              {sections.data?.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.code}
                </option>
              ))}
            </select>
          </label>
        </div>
        <span className="spacer" />
        {canEdit && (
          <button className="btn primary" onClick={() => setEditing('new')}>
            Add offering
          </button>
        )}
      </div>

      <ErrorBox message={error ?? list.error} />
      <WarningBox items={warnings} onClose={() => setWarnings([])} />

      {editing && (
        <OfferingForm
          offering={editing === 'new' ? null : editing}
          years={years.data ?? []}
          sections={sections.data ?? []}
          onDone={(w) => {
            setEditing(null)
            setWarnings(w)
            list.reload()
            if (w.length) window.scrollTo({ top: 0, behavior: 'smooth' })
          }}
          onCancel={() => setEditing(null)}
        />
      )}

      {changesFor && <ClassChanges offering={changesFor} onClose={() => setChangesFor(null)} />}

      {!list.data ? (
        <Loading />
      ) : list.data.length === 0 ? (
        <Empty>No course offerings match these filters.</Empty>
      ) : (
        <table className="table">
          <thead>
            <tr>
              <th>Course</th>
              <th>Sem</th>
              <th>Section</th>
              <th>Year</th>
              <th>Teachers</th>
              <th>Weekly timetable</th>
              <th className="num">Students</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {list.data.map((o) => (
              <tr key={o.id}>
                <td>
                  <strong>{o.course.code}</strong> {o.course.name}
                </td>
                <td>{o.semester}</td>
                <td>{o.section.code}</td>
                <td className="year-cell">{o.academicYear.name}</td>
                <td className="small">
                  {o.teachers.map((t) => (
                    <div key={t.facultyId + t.role}>
                      {t.name} <span className="muted">({label(t.role)})</span>
                    </div>
                  ))}
                </td>
                <td className="small">
                  {o.slots.length === 0
                    ? '-'
                    : o.slots.map((s) => (
                        <div key={s.id}>
                          {day(s.day)} {time(s.startTime)}-{time(s.endTime)} {s.room}
                        </div>
                      ))}
                </td>
                <td className="num">{o.enrolled}</td>
                <td className="actions">
                  <button className="link-btn" onClick={() => setChangesFor(o)}>
                    Class changes
                  </button>
                  {canEdit && (
                    <>
                      <button className="link-btn" onClick={() => setEditing(o)}>
                        Edit
                      </button>
                      <button className="link-btn danger" onClick={() => remove(o)}>
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

interface TeacherRow {
  facultyId: string
  role: TeachingRole
}

function OfferingForm({
  offering,
  years,
  sections,
  onDone,
  onCancel,
}: {
  offering: Offering | null
  years: AcademicYear[]
  sections: Section[]
  onDone: (warnings: string[]) => void
  onCancel: () => void
}) {
  const courses = useLoad<Course[]>('/courses')
  const faculty = useLoad<Faculty[]>('/faculty')
  const currentYear = years.find((y) => y.current)
  const [courseId, setCourseId] = useState(str(offering?.course.id))
  const [yearId, setYearId] = useState(str(offering?.academicYear.id ?? currentYear?.id))
  const [semester, setSemester] = useState(str(offering?.semester ?? 1))
  const [sectionId, setSectionId] = useState(str(offering?.section.id))
  const [teachers, setTeachers] = useState<TeacherRow[]>(
    offering ? offering.teachers.map((t) => ({ facultyId: String(t.facultyId), role: t.role })) : [{ facultyId: '', role: 'THEORY' }],
  )
  const [slots, setSlots] = useState<Slot[]>(
    offering ? offering.slots.map((s) => ({ ...s, startTime: time(s.startTime), endTime: time(s.endTime) })) : [],
  )
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  async function submit(e: FormEvent) {
    e.preventDefault()
    setBusy(true)
    setError(null)
    const body = {
      courseId: Number(courseId),
      academicYearId: Number(yearId),
      semester: Number(semester),
      sectionId: Number(sectionId),
      teachers: teachers.filter((t) => t.facultyId).map((t) => ({ facultyId: Number(t.facultyId), role: t.role })),
      slots: slots.map(({ day, startTime, endTime, room, type }) => ({ day, startTime, endTime, room, type })),
    }
    try {
      const res = offering
        ? await api.put<{ warnings: string[] }>(`/offerings/${offering.id}`, body)
        : await api.post<{ warnings: string[] }>('/offerings', body)
      onDone(res.warnings)
    } catch (err) {
      const fe = err instanceof ApiError ? Object.values(err.fieldErrors) : []
      setError([(err as Error).message, ...fe].join('. '))
    } finally {
      setBusy(false)
    }
  }

  const activeFaculty = (faculty.data ?? []).filter((f) => f.status !== 'LEFT')

  return (
    <form className="card form" onSubmit={submit}>
      <h2>{offering ? `Edit ${offering.course.code}` : 'New course offering'}</h2>
      <ErrorBox message={error} />
      <div className="form-grid">
        <label className="wide">
          Course
          <select value={courseId} onChange={(e) => setCourseId(e.target.value)} required>
            <option value="">Choose...</option>
            {courses.data?.map((c) => (
              <option key={c.id} value={c.id}>
                {c.code} {c.name}
              </option>
            ))}
          </select>
        </label>
        <label>
          Academic year
          <select value={yearId} onChange={(e) => setYearId(e.target.value)} required>
            <option value="">Choose...</option>
            {years.map((y) => (
              <option key={y.id} value={y.id}>
                {y.name}
              </option>
            ))}
          </select>
        </label>
        <label>
          Semester
          <input type="number" min={1} max={12} value={semester} onChange={(e) => setSemester(e.target.value)} required />
        </label>
        <label>
          Section
          <select value={sectionId} onChange={(e) => setSectionId(e.target.value)} required>
            <option value="">Choose...</option>
            {sections.map((s) => (
              <option key={s.id} value={s.id}>
                {s.code}
              </option>
            ))}
          </select>
        </label>
      </div>

      <h3>Teachers</h3>
      {teachers.map((t, i) => (
        <div className="row-edit" key={i}>
          <select
            value={t.facultyId}
            onChange={(e) => setTeachers(teachers.map((x, j) => (j === i ? { ...x, facultyId: e.target.value } : x)))}
          >
            <option value="">Choose teacher...</option>
            {activeFaculty.map((f) => (
              <option key={f.id} value={f.id}>
                {f.name} ({f.department.code})
              </option>
            ))}
          </select>
          <select
            value={t.role}
            onChange={(e) => setTeachers(teachers.map((x, j) => (j === i ? { ...x, role: e.target.value as TeachingRole } : x)))}
          >
            {ROLES.map((r) => (
              <option key={r} value={r}>
                {label(r)}
              </option>
            ))}
          </select>
          <button type="button" className="link-btn danger" onClick={() => setTeachers(teachers.filter((_, j) => j !== i))}>
            Remove
          </button>
        </div>
      ))}
      <button type="button" className="link-btn" onClick={() => setTeachers([...teachers, { facultyId: '', role: 'LAB' }])}>
        + Add teacher
      </button>

      <h3>Weekly timetable</h3>
      {slots.length === 0 && <p className="muted small">No weekly classes yet.</p>}
      {slots.map((s, i) => {
        const set = (patch: Partial<Slot>) => setSlots(slots.map((x, j) => (j === i ? { ...x, ...patch } : x)))
        return (
          <div className="row-edit" key={i}>
            <select value={s.day} onChange={(e) => set({ day: e.target.value as Day })}>
              {DAYS.map((d) => (
                <option key={d} value={d}>
                  {label(d)}
                </option>
              ))}
            </select>
            <input type="time" value={s.startTime} onChange={(e) => set({ startTime: e.target.value })} required />
            <span>to</span>
            <input type="time" value={s.endTime} onChange={(e) => set({ endTime: e.target.value })} required />
            <input placeholder="Room" value={s.room} onChange={(e) => set({ room: e.target.value })} required />
            <select value={s.type} onChange={(e) => set({ type: e.target.value as TeachingRole })} title="Kind of class">
              {ROLES.map((r) => (
                <option key={r} value={r}>
                  {label(r)}
                </option>
              ))}
            </select>
            <button type="button" className="link-btn danger" onClick={() => setSlots(slots.filter((_, j) => j !== i))}>
              Remove
            </button>
          </div>
        )
      })}
      <button
        type="button"
        className="link-btn"
        onClick={() => setSlots([...slots, { day: 'MONDAY', startTime: '09:00', endTime: '10:00', room: '', type: 'THEORY' }])}
      >
        + Add weekly class
      </button>

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

function ClassChanges({ offering, onClose }: { offering: Offering; onClose: () => void }) {
  const { canEdit } = useAuth()
  const list = useLoad<ClassChange[]>(`/offerings/${offering.id}/exceptions`)
  const [form, setForm] = useState({ date: '', type: 'CANCELLED', startTime: '', endTime: '', room: '', reason: '' })
  const [error, setError] = useState<string | null>(null)

  async function add(e: FormEvent) {
    e.preventDefault()
    setError(null)
    try {
      await api.post(`/offerings/${offering.id}/exceptions`, {
        ...form,
        endTime: form.endTime || null,
        room: form.room || null,
      })
      setForm({ ...form, date: '', reason: '' })
      list.reload()
    } catch (err) {
      setError((err as Error).message)
    }
  }

  async function remove(c: ClassChange) {
    try {
      await api.delete(`/class-exceptions/${c.id}`)
      list.reload()
    } catch (err) {
      setError((err as Error).message)
    }
  }

  const cancelling = form.type === 'CANCELLED'

  return (
    <div className="card form">
      <div className="section-head">
        <h2>
          Class changes: {offering.course.code}, Sem {offering.semester} {offering.section.code}
        </h2>
        <span className="spacer" />
        <button className="link-btn" onClick={onClose}>
          Close
        </button>
      </div>
      <ErrorBox message={error ?? list.error} />
      {list.data && list.data.length === 0 && <p className="muted small">No cancelled or extra classes.</p>}
      {list.data && list.data.length > 0 && (
        <table className="table compact">
          <tbody>
            {list.data.map((c) => (
              <tr key={c.id}>
                <td>{date(c.date)}</td>
                <td>{c.type === 'CANCELLED' ? <span className="bad">Cancelled</span> : <span className="good">Extra</span>}</td>
                <td>
                  {time(c.startTime)}
                  {c.endTime ? '-' + time(c.endTime) : ''} {c.room}
                </td>
                <td>{c.reason}</td>
                <td className="actions">
                  {canEdit && (
                    <button className="link-btn danger" onClick={() => remove(c)}>
                      Remove
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
      {canEdit && (
        <form onSubmit={add}>
          <div className="form-grid">
            <label>
              Change
              <select value={form.type} onChange={(e) => setForm({ ...form, type: e.target.value })}>
                <option value="CANCELLED">Cancel one class</option>
                <option value="EXTRA">Add an extra class</option>
              </select>
            </label>
            <label>
              Date
              <input type="date" value={form.date} onChange={(e) => setForm({ ...form, date: e.target.value })} required />
            </label>
            {cancelling ? (
              <label>
                Class
                <select value={form.startTime} onChange={(e) => setForm({ ...form, startTime: e.target.value })} required>
                  <option value="">Choose...</option>
                  {offering.slots.map((s) => (
                    <option key={s.id} value={time(s.startTime)}>
                      {day(s.day)} {time(s.startTime)}-{time(s.endTime)} {s.room}
                    </option>
                  ))}
                </select>
              </label>
            ) : (
              <>
                <label>
                  Starts
                  <input type="time" value={form.startTime} onChange={(e) => setForm({ ...form, startTime: e.target.value })} required />
                </label>
                <label>
                  Ends
                  <input type="time" value={form.endTime} onChange={(e) => setForm({ ...form, endTime: e.target.value })} required />
                </label>
                <label>
                  Room
                  <input value={form.room} onChange={(e) => setForm({ ...form, room: e.target.value })} required />
                </label>
              </>
            )}
            <label className="wide">
              Reason
              <input value={form.reason} onChange={(e) => setForm({ ...form, reason: e.target.value })} />
            </label>
          </div>
          <div className="form-actions">
            <button className="btn primary" type="submit">
              {cancelling ? 'Cancel this class' : 'Add extra class'}
            </button>
          </div>
        </form>
      )}
    </div>
  )
}
