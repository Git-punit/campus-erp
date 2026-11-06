import { Link } from 'react-router'
import { useAuth } from '../auth'
import { Empty, ErrorBox, Loading, Money, UsageBar } from '../components/ui'
import { date, time } from '../lib/format'
import type { CalendarEntry, EventStatus } from '../lib/types'
import { useLoad } from '../lib/useLoad'

interface Dashboard {
  today: string
  semester: { academicYear: string; half: 'ODD' | 'EVEN' } | null
  holidayToday: string | null
  faculty: number
  students: number
  coursesThisSemester: number
  todaysClasses: CalendarEntry[]
  projects: { ongoing: number; sanctioned: number; utilized: number; percentUsed: number }
  training: { clientsReached: number; proposals: number; accepted: number; conducted: number }
  budgets: { financialYear: string; allocated: number; utilized: number; percentUsed: number; overspent: number }
  pendingApprovals: { count: number; amount: number }
  upcomingEvents: { id: number; name: string; type: string; startDate: string; endDate: string; venue: string | null; status: EventStatus }[]
}

export default function DashboardPage() {
  const { isAdmin } = useAuth()
  const { data, error } = useLoad<Dashboard>('/dashboard')
  if (error) return <ErrorBox message={error} />
  if (!data) return <Loading />
  const d = data

  return (
    <>
      <div className="page-head">
        <h1>Dashboard</h1>
        <span className="muted">
          {date(d.today)}
          {d.semester && ` · ${d.semester.academicYear}, ${d.semester.half === 'ODD' ? 'odd' : 'even'} semesters`}
        </span>
      </div>

      <div className="stats">
        <Link to="/faculty" className="stat">
          <span>Faculty</span>
          <strong>{d.faculty}</strong>
          <small>active</small>
        </Link>
        <Link to="/students" className="stat">
          <span>Students</span>
          <strong>{d.students}</strong>
          <small>active</small>
        </Link>
        <Link to="/courses" className="stat">
          <span>Courses this semester</span>
          <strong>{d.coursesThisSemester}</strong>
          <small>offerings</small>
        </Link>
        <Link to="/budgets" className={'stat' + (d.pendingApprovals.count ? ' attention' : '')}>
          <span>Waiting for approval</span>
          <strong>{d.pendingApprovals.count}</strong>
          <small>
            <Money value={d.pendingApprovals.amount} />
            {isAdmin && d.pendingApprovals.count > 0 ? ' · review' : ''}
          </small>
        </Link>
      </div>

      <div className="dash-grid">
        <section className="card">
          <h2>Today's classes</h2>
          {d.holidayToday ? (
            <Empty>Holiday today: {d.holidayToday}. No classes.</Empty>
          ) : d.todaysClasses.length === 0 ? (
            <Empty>No classes today.</Empty>
          ) : (
            <TodaysClasses entries={d.todaysClasses} />
          )}
        </section>

        <div className="dash-side">
          <section className="card">
            <h2>Research project funds</h2>
            <p className="muted small">{d.projects.ongoing} ongoing projects</p>
            <div className="money-line">
              <Money value={d.projects.utilized} /> used of <Money value={d.projects.sanctioned} />
            </div>
            <UsageBar percent={d.projects.percentUsed} />
            <Link className="small" to="/projects">Projects</Link>
          </section>

          <section className="card">
            <h2>Budgets {d.budgets.financialYear}</h2>
            <div className="money-line">
              <Money value={d.budgets.utilized} /> used of <Money value={d.budgets.allocated} />
            </div>
            <UsageBar percent={d.budgets.percentUsed} />
            {d.budgets.overspent > 0 && (
              <p className="flag over">
                {d.budgets.overspent} budget{d.budgets.overspent > 1 ? 's' : ''} overspent
              </p>
            )}
            <Link className="small" to="/budgets">Budgets</Link>
          </section>

          <section className="card">
            <h2>Training pipeline</h2>
            <div className="pipeline">
              <div><strong>{d.training.clientsReached}</strong><span>reached</span></div>
              <div><strong>{d.training.proposals}</strong><span>proposals</span></div>
              <div><strong>{d.training.accepted}</strong><span>accepted</span></div>
              <div><strong>{d.training.conducted}</strong><span>conducted</span></div>
            </div>
            <Link className="small" to="/training">Training programs</Link>
          </section>

          <section className="card">
            <h2>Upcoming events</h2>
            {d.upcomingEvents.length === 0 ? (
              <Empty>Nothing in the next 60 days.</Empty>
            ) : (
              <ul className="plain">
                {d.upcomingEvents.map((e) => (
                  <li key={e.id}>
                    <strong>{e.name}</strong>
                    <div className="muted small">
                      {e.startDate === e.endDate ? date(e.startDate) : `${date(e.startDate)} to ${date(e.endDate)}`}
                      {e.venue ? `, ${e.venue}` : ''}
                      {e.status === 'ONGOING' ? ' · on now' : ''}
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </section>
        </div>
      </div>
    </>
  )
}

function TodaysClasses({ entries }: { entries: CalendarEntry[] }) {
  return (
    <table className="table compact">
      <thead>
        <tr>
          <th>Time</th>
          <th>Course</th>
          <th>Group</th>
          <th>Room</th>
          <th>Teacher</th>
        </tr>
      </thead>
      <tbody>
        {entries.map((e, i) => (
          <tr key={i} className={e.status === 'CANCELLED' ? 'cancelled' : undefined}>
            <td>
              {time(e.startTime)}-{time(e.endTime)}
            </td>
            <td>
              {e.course.code} {e.course.name}
              {e.status === 'CANCELLED' && <span className="flag over small"> cancelled</span>}
              {e.status === 'EXTRA' && <span className="flag ok small"> extra</span>}
            </td>
            <td>
              Sem {e.semester} {e.section.code}
            </td>
            <td>{e.room}</td>
            <td className="small">{e.teachers.join(', ')}</td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}
