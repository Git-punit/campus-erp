import { useState } from 'react'
import { Link, useParams, useSearchParams } from 'react-router'
import CalendarView from '../components/CalendarView'
import { Badge, Empty, ErrorBox, Loading, Money, UsageBar } from '../components/ui'
import { day, label, time } from '../lib/format'
import type { AcademicYear, Faculty, Offering, Project } from '../lib/types'
import { useLoad } from '../lib/useLoad'

export default function FacultyDetailPage() {
  const { id } = useParams()
  const [params, setParams] = useSearchParams()
  const tab = params.get('tab') ?? 'courses'
  const teacher = useLoad<Faculty>(`/faculty/${id}`)

  if (teacher.error) return <ErrorBox message={teacher.error} />
  if (!teacher.data) return <Loading />
  const f = teacher.data

  return (
    <>
      <p className="crumbs">
        <Link to="/faculty">Faculty</Link> /
      </p>
      <h1>{f.name}</h1>
      <div className="card facts">
        <div><span>Employee ID</span>{f.employeeId}</div>
        <div><span>Department</span>{f.department.name}</div>
        <div><span>Designation</span>{f.designation ?? '-'}</div>
        <div><span>Email</span>{f.email ?? '-'}</div>
        <div><span>Phone</span>{f.phone ?? '-'}</div>
        <div><span>Status</span><Badge value={f.status} /></div>
      </div>

      <div className="tabs">
        {[
          ['courses', `Courses (${f.courseCount})`],
          ['projects', `Projects (${f.projectCount})`],
          ['calendar', 'Calendar'],
        ].map(([key, text]) => (
          <button key={key} className={tab === key ? 'tab active' : 'tab'} onClick={() => setParams({ tab: key })}>
            {text}
          </button>
        ))}
      </div>

      {tab === 'courses' && <TeacherCourses facultyId={f.id} />}
      {tab === 'projects' && <TeacherProjects facultyId={f.id} />}
      {tab === 'calendar' && <CalendarView kind="faculty" id={f.id} />}
    </>
  )
}

function TeacherCourses({ facultyId }: { facultyId: number }) {
  const years = useLoad<AcademicYear[]>('/academic-years')
  const current = years.data?.find((y) => y.current)?.name ?? ''
  const [year, setYear] = useState<string | null>(null)
  const chosen = year ?? current
  const courses = useLoad<Offering[]>(years.data ? `/faculty/${facultyId}/courses?year=${encodeURIComponent(chosen)}` : null)

  return (
    <>
      <div className="filters">
        <label>
          Academic year
          <select value={chosen} onChange={(e) => setYear(e.target.value)}>
            <option value="">All years</option>
            {years.data?.map((y) => (
              <option key={y.id} value={y.name}>
                {y.name}
                {y.current ? ' (current)' : ''}
              </option>
            ))}
          </select>
        </label>
      </div>
      <ErrorBox message={courses.error} />
      {!courses.data ? (
        <Loading />
      ) : courses.data.length === 0 ? (
        <Empty>No courses for this year.</Empty>
      ) : (
        <table className="table">
          <thead>
            <tr>
              <th>#</th>
              <th>Course</th>
              <th>Semester</th>
              <th>Section</th>
              <th>Academic year</th>
              <th>Role</th>
              <th>Weekly classes</th>
            </tr>
          </thead>
          <tbody>
            {courses.data.map((o, i) => (
              <tr key={o.id}>
                <td>{i + 1}</td>
                <td>
                  {o.course.code} {o.course.name}
                </td>
                <td>Sem {o.semester}</td>
                <td>{o.section.code}</td>
                <td>{o.academicYear.name}</td>
                <td>
                  {o.teachers
                    .filter((t) => t.facultyId === facultyId)
                    .map((t) => label(t.role))
                    .join(', ')}
                </td>
                <td className="small">
                  {o.slots.map((s) => `${day(s.day)} ${time(s.startTime)}`).join(', ') || '-'}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </>
  )
}

function TeacherProjects({ facultyId }: { facultyId: number }) {
  const projects = useLoad<Project[]>(`/faculty/${facultyId}/projects`)
  if (projects.error) return <ErrorBox message={projects.error} />
  if (!projects.data) return <Loading />
  if (projects.data.length === 0) return <Empty>Not on any research projects.</Empty>
  return (
    <table className="table">
      <thead>
        <tr>
          <th>Project</th>
          <th>Role</th>
          <th>Funding agency</th>
          <th className="num">Amount</th>
          <th className="num">Utilized</th>
          <th className="num">Balance</th>
          <th>Used</th>
        </tr>
      </thead>
      <tbody>
        {projects.data.map((p) => (
          <tr key={p.id}>
            <td>
              <Link to={`/projects?open=${p.id}`}>{p.title}</Link>
            </td>
            <td>{p.team.find((m) => m.facultyId === facultyId)?.role === 'PI' ? 'PI' : 'Co-investigator'}</td>
            <td>{p.fundingAgency}</td>
            <td className="num"><Money value={p.sanctionedAmount} /></td>
            <td className="num"><Money value={p.utilized} /></td>
            <td className="num"><Money value={p.balance} /></td>
            <td><UsageBar percent={p.percentUsed} /></td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}
