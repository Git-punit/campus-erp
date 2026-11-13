import { useState } from 'react'
import { useParams, useSearchParams } from 'react-router'
import { api } from '../api'
import { useAuth } from '../auth'
import CalendarView from '../components/CalendarView'
import { Badge, Empty, ErrorBox, Loading } from '../components/ui'
import { day, label, time } from '../lib/format'
import type { AcademicYear, Offering, Student } from '../lib/types'
import { useLoad } from '../lib/useLoad'

export default function StudentDetailPage() {
  const { id } = useParams()
  const [params, setParams] = useSearchParams()
  const tab = params.get('tab') ?? 'courses'
  const student = useLoad<Student>(`/students/${id}`)

  if (student.error) return <ErrorBox message={student.error} />
  if (!student.data) return <Loading />
  const s = student.data

  return (
    <>
      <h1>{s.name}</h1>
      <div className="card facts">
        <div><span>Roll no</span>{s.rollNo}</div>
        <div><span>Programme</span>{s.programme.name}</div>
        <div><span>Semester</span>{s.currentSemester}</div>
        <div><span>Section</span>{s.section.code}</div>
        <div><span>Email</span>{s.email ?? '-'}</div>
        <div><span>Status</span><Badge value={s.status} /></div>
      </div>
      <div className="tabs">
        {[
          ['courses', 'Courses'],
          ['calendar', 'Calendar'],
        ].map(([key, text]) => (
          <button key={key} className={tab === key ? 'tab active' : 'tab'} onClick={() => setParams({ tab: key })}>
            {text}
          </button>
        ))}
      </div>
      {tab === 'courses' && <StudentCourses student={s} />}
      {tab === 'calendar' && <CalendarView kind="student" id={s.id} />}
    </>
  )
}

function StudentCourses({ student }: { student: Student }) {
  const { canEdit } = useAuth()
  const years = useLoad<AcademicYear[]>('/academic-years')
  const [year, setYear] = useState<string | null>(null)
  const chosen = year ?? years.data?.find((y) => y.current)?.name ?? ''
  const courses = useLoad<Offering[]>(years.data ? `/students/${student.id}/courses?year=${encodeURIComponent(chosen)}` : null)
  const available = useLoad<Offering[]>(chosen ? `/offerings?year=${encodeURIComponent(chosen)}` : null)
  const [adding, setAdding] = useState('')
  const [error, setError] = useState<string | null>(null)

  async function add() {
    if (!adding) return
    setError(null)
    try {
      await api.post(`/students/${student.id}/courses`, { offeringId: Number(adding) })
      setAdding('')
      courses.reload()
    } catch (e) {
      setError((e as Error).message)
    }
  }

  async function remove(o: Offering) {
    if (!confirm(`Remove ${o.course.code} from ${student.name}'s courses?`)) return
    setError(null)
    try {
      await api.delete(`/students/${student.id}/courses/${o.id}`)
      courses.reload()
    } catch (e) {
      setError((e as Error).message)
    }
  }

  const enrolledIds = new Set(courses.data?.map((o) => o.id))
  // the student's own section and semester first
  const choices = (available.data ?? [])
    .filter((o) => !enrolledIds.has(o.id))
    .sort((a, b) => {
      const own = (o: Offering) => (o.section.id === student.section.id && o.semester === student.currentSemester ? 0 : 1)
      return own(a) - own(b) || a.course.code.localeCompare(b.course.code)
    })

  return (
    <>
      <div className="section-head">
        <div className="filters">
          <label>
            Academic year
            <select value={chosen} onChange={(e) => setYear(e.target.value)}>
              <option value="">All years</option>
              {years.data?.map((y) => (
                <option key={y.id} value={y.name}>
                  {y.name}
                </option>
              ))}
            </select>
          </label>
        </div>
        <span className="spacer" />
        {canEdit && chosen && student.status === 'ACTIVE' && (
          <div className="inline-add">
            <select value={adding} onChange={(e) => setAdding(e.target.value)}>
              <option value="">Add a course...</option>
              {choices.map((o) => (
                <option key={o.id} value={o.id}>
                  {o.course.code} {o.course.name} (Sem {o.semester} {o.section.code})
                </option>
              ))}
            </select>
            <button className="btn" onClick={add} disabled={!adding}>
              Add
            </button>
          </div>
        )}
      </div>
      <ErrorBox message={error ?? courses.error} />
      {!courses.data ? (
        <Loading />
      ) : courses.data.length === 0 ? (
        <Empty>Not enrolled in any courses for this year.</Empty>
      ) : (
        <table className="table">
          <thead>
            <tr>
              <th>Course</th>
              <th className="num">Credits</th>
              <th>Teachers</th>
              <th>Weekly classes</th>
              <th>Group</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {courses.data.map((o) => (
              <tr key={o.id}>
                <td>
                  <strong>{o.course.code}</strong> {o.course.name}
                </td>
                <td className="num">{o.credits}</td>
                <td className="small">
                  {o.teachers.map((t) => (
                    <div key={t.facultyId + t.role}>
                      {t.name} <span className="muted">({label(t.role)})</span>
                    </div>
                  ))}
                </td>
                <td className="small">
                  {o.slots.map((s) => (
                    <div key={s.id}>
                      {day(s.day)} {time(s.startTime)}-{time(s.endTime)}, {s.room}
                    </div>
                  ))}
                </td>
                <td className="small">
                  Sem {o.semester} {o.section.code}, {o.academicYear.name}
                </td>
                <td className="actions">
                  {canEdit && (
                    <button className="link-btn danger" onClick={() => remove(o)}>
                      Remove
                    </button>
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
