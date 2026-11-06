import { useState } from 'react'
import CalendarView from '../components/CalendarView'
import type { Faculty, Student } from '../lib/types'
import { useLoad } from '../lib/useLoad'

/** Pick a student or a teacher and see their timetable as a calendar. */
export default function CalendarPage() {
  const [kind, setKind] = useState<'student' | 'faculty'>('student')
  const [id, setId] = useState('')
  const students = useLoad<Student[]>('/students')
  const faculty = useLoad<Faculty[]>('/faculty')

  return (
    <>
      <h1>Calendar</h1>
      <div className="filters">
        <label>
          Show for
          <select
            value={kind}
            onChange={(e) => {
              setKind(e.target.value as 'student' | 'faculty')
              setId('')
            }}
          >
            <option value="student">A student</option>
            <option value="faculty">A teacher</option>
          </select>
        </label>
        <label className="wide-select">
          {kind === 'student' ? 'Student' : 'Teacher'}
          <select value={id} onChange={(e) => setId(e.target.value)}>
            <option value="">Choose...</option>
            {kind === 'student'
              ? students.data
                  ?.filter((s) => s.status === 'ACTIVE')
                  .map((s) => (
                    <option key={s.id} value={s.id}>
                      {s.rollNo} {s.name} (Sem {s.currentSemester} {s.section.code})
                    </option>
                  ))
              : faculty.data
                  ?.filter((f) => f.status !== 'LEFT')
                  .map((f) => (
                    <option key={f.id} value={f.id}>
                      {f.name} ({f.department.code})
                    </option>
                  ))}
          </select>
        </label>
      </div>
      {id ? (
        <CalendarView key={kind + id} kind={kind} id={Number(id)} />
      ) : (
        <p className="muted">
          Choose someone to see their classes. Weekly classes repeat through the semester, holidays are skipped, and
          cancelled or extra classes show on their day.
        </p>
      )}
    </>
  )
}
