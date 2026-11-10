import { useState, type FormEvent } from 'react'
import { Link } from 'react-router'
import { api } from '../api'
import { useAuth } from '../auth'
import ImportBox from '../components/ImportBox'
import CrudSection, { enumOptions, optionsOf, str, type CrudConfig } from '../components/CrudSection'
import { Badge, ErrorBox } from '../components/ui'
import type { AcademicYear, Section, Student } from '../lib/types'
import { useLoad } from '../lib/useLoad'

export const STUDENT_STATUSES = ['ACTIVE', 'ON_BREAK', 'DROPPED', 'GRADUATED']

export default function StudentsPage() {
  const { canEdit } = useAuth()
  const sections = useLoad<Section[]>('/sections')
  const [sectionId, setSectionId] = useState('')
  const [semester, setSemester] = useState('')
  const [showEnrol, setShowEnrol] = useState(false)
  const [showImport, setShowImport] = useState(false)
  const [version, setVersion] = useState(0) // bump to reload the list after an import

  const query = new URLSearchParams()
  if (sectionId) query.set('sectionId', sectionId)
  if (semester) query.set('semester', semester)

  const config: CrudConfig<Student> = {
    noun: 'student',
    listPath: `/students?${query}`,
    savePath: '/students',
    columns: [
      { label: 'Roll no', render: (s) => s.rollNo },
      { label: 'Name', render: (s) => <Link to={`/students/${s.id}`}>{s.name}</Link> },
      { label: 'Programme', render: (s) => s.programme.code },
      { label: 'Sem', className: 'num', render: (s) => s.currentSemester },
      { label: 'Section', render: (s) => s.section.code },
      { label: 'Email', render: (s) => s.email ?? '-' },
      { label: 'Status', render: (s) => <Badge value={s.status} /> },
    ],
    fields: [
      { name: 'rollNo', label: 'Roll number', required: true },
      { name: 'name', label: 'Name', required: true },
      { name: 'sectionId', label: 'Section', type: 'select', required: true, options: optionsOf(sections.data, (s) => `${s.code} (${s.programme.code})`) },
      { name: 'currentSemester', label: 'Current semester', type: 'number', required: true },
      { name: 'email', label: 'Email', type: 'email' },
      { name: 'phone', label: 'Phone' },
      { name: 'status', label: 'Status', type: 'select', required: true, options: enumOptions(STUDENT_STATUSES) },
    ],
    empty: { rollNo: '', name: '', sectionId: '', currentSemester: '1', email: '', phone: '', status: 'ACTIVE' },
    csv: {
      fileName: 'students',
      row: (s) => ({
        roll_no: s.rollNo,
        name: s.name,
        programme: s.programme.code,
        semester: s.currentSemester,
        section: s.section.code,
        email: s.email,
        phone: s.phone,
        status: s.status,
      }),
    },
    toForm: (s) => ({
      rollNo: s.rollNo,
      name: s.name,
      sectionId: str(s.section.id),
      currentSemester: str(s.currentSemester),
      email: str(s.email),
      phone: str(s.phone),
      status: s.status,
    }),
    headerExtra: (
      <div className="filters">
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
        <label>
          Semester
          <select value={semester} onChange={(e) => setSemester(e.target.value)}>
            <option value="">All</option>
            {[1, 2, 3, 4, 5, 6, 7, 8].map((n) => (
              <option key={n}>{n}</option>
            ))}
          </select>
        </label>
        {canEdit && (
          <>
            <button className="btn" type="button" onClick={() => setShowEnrol(!showEnrol)}>
              Enrol a whole section
            </button>
            <button className="btn" type="button" onClick={() => setShowImport(!showImport)}>
              Import CSV
            </button>
          </>
        )}
      </div>
    ),
  }

  return (
    <>
      <h1>Students</h1>
      {showEnrol && <SectionEnrol sections={sections.data ?? []} onClose={() => setShowEnrol(false)} />}
      {showImport && (
        <ImportBox
          what="students"
          endpoint="/students/import"
          required={['roll_no', 'name', 'section', 'semester']}
          optional={['email', 'phone', 'status']}
          example={{ roll_no: '26CSE101', name: 'Asha Rao', section: 'CSE-A', semester: '1', email: 'asha@example.edu', status: 'ACTIVE' }}
          onDone={() => setVersion((v) => v + 1)}
          onClose={() => setShowImport(false)}
        />
      )}
      <CrudSection config={config} key={config.listPath + version} />
    </>
  )
}

function SectionEnrol({ sections, onClose }: { sections: Section[]; onClose: () => void }) {
  const years = useLoad<AcademicYear[]>('/academic-years')
  const [yearId, setYearId] = useState('')
  const [semester, setSemester] = useState('1')
  const [sectionId, setSectionId] = useState('')
  const [result, setResult] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const chosenYear = yearId || str(years.data?.find((y) => y.current)?.id)

  async function submit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setResult(null)
    try {
      const r = await api.post<{ students: number; offerings: number; added: number; alreadyEnrolled: number }>(
        '/enrolments/section',
        { academicYearId: Number(chosenYear), semester: Number(semester), sectionId: Number(sectionId) },
      )
      setResult(
        `${r.students} active students and ${r.offerings} courses: ${r.added} enrolments added` +
          (r.alreadyEnrolled ? `, ${r.alreadyEnrolled} were already there.` : '.'),
      )
    } catch (err) {
      setError((err as Error).message)
    }
  }

  return (
    <form className="card form" onSubmit={submit}>
      <div className="section-head">
        <h2>Enrol a whole section</h2>
        <span className="spacer" />
        <button type="button" className="link-btn" onClick={onClose}>
          Close
        </button>
      </div>
      <p className="muted small">
        Puts every active student of the section and semester into all of that section's course offerings for the
        year. Running it again only adds what's missing.
      </p>
      <ErrorBox message={error} />
      {result && <div className="ok-box">{result}</div>}
      <div className="form-grid">
        <label>
          Academic year
          <select value={chosenYear} onChange={(e) => setYearId(e.target.value)} required>
            {years.data?.map((y) => (
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
      <div className="form-actions">
        <button className="btn primary" type="submit">
          Enrol
        </button>
      </div>
    </form>
  )
}
