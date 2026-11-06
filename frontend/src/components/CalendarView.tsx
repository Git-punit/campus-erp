import type { DatesSetArg, EventClickArg, EventInput } from '@fullcalendar/core'
import enGb from '@fullcalendar/core/locales/en-gb'
import dayGridPlugin from '@fullcalendar/daygrid'
import FullCalendar from '@fullcalendar/react'
import timeGridPlugin from '@fullcalendar/timegrid'
import { useCallback, useState } from 'react'
import { api } from '../api'
import { date, isoDate, time } from '../lib/format'
import type { CalendarData, CalendarEntry } from '../lib/types'
import { ErrorBox } from './ui'

/**
 * Day / Week / Month calendar for one student or one teacher. Built entirely
 * from enrolments and timetables on the server; nothing here is stored.
 */
export default function CalendarView({ kind, id }: { kind: 'student' | 'faculty'; id: number }) {
  const [events, setEvents] = useState<EventInput[]>([])
  const [error, setError] = useState<string | null>(null)
  const [picked, setPicked] = useState<CalendarEntry | null>(null)

  const load = useCallback(
    (arg: DatesSetArg) => {
      const from = isoDate(arg.start)
      const last = new Date(arg.end)
      last.setDate(last.getDate() - 1)
      api
        .get<CalendarData>(`/calendar/${kind}/${id}?from=${from}&to=${isoDate(last)}`)
        .then((data) => {
          setError(null)
          setEvents([
            ...data.entries.map(
              (e): EventInput => ({
                title: `${e.course.code} ${e.status === 'CANCELLED' ? '(cancelled)' : ''}`.trim(),
                start: `${e.date}T${e.startTime}`,
                end: `${e.date}T${e.endTime}`,
                classNames: ['cal-' + e.status.toLowerCase()],
                extendedProps: { entry: e },
              }),
            ),
            ...data.holidays.map(
              (h): EventInput => ({
                title: h.name,
                start: h.date,
                allDay: true,
                display: 'background',
                classNames: ['cal-holiday'],
              }),
            ),
            ...data.holidays.map(
              (h): EventInput => ({ title: 'Holiday: ' + h.name, start: h.date, allDay: true, classNames: ['cal-holiday-label'] }),
            ),
          ])
        })
        .catch((e: Error) => setError(e.message))
    },
    [kind, id],
  )

  function click(arg: EventClickArg) {
    const entry = arg.event.extendedProps.entry as CalendarEntry | undefined
    if (entry) setPicked(entry)
  }

  return (
    <div className="card calendar">
      <ErrorBox message={error} />
      <FullCalendar
        plugins={[dayGridPlugin, timeGridPlugin]}
        locale={enGb}
        initialView="timeGridWeek"
        headerToolbar={{ left: 'prev,next today', center: 'title', right: 'timeGridDay,timeGridWeek,dayGridMonth' }}
        buttonText={{ today: 'Today', day: 'Day', week: 'Week', month: 'Month' }}
        firstDay={1}
        weekends
        slotMinTime="08:00:00"
        slotMaxTime="18:00:00"
        allDaySlot
        height="auto"
        events={events}
        datesSet={load}
        eventClick={click}
        eventTimeFormat={{ hour: '2-digit', minute: '2-digit', hour12: false }}
        slotLabelFormat={{ hour: '2-digit', minute: '2-digit', hour12: false }}
      />
      {picked && (
        <div className="cal-detail">
          <button className="link-btn close" onClick={() => setPicked(null)}>
            Close
          </button>
          <strong>
            {picked.course.code} {picked.course.name}
          </strong>
          <div>
            {date(picked.date)}, {time(picked.startTime)}-{time(picked.endTime)}, {picked.room}
          </div>
          <div>
            Sem {picked.semester} {picked.section.code} &middot; {picked.teachers.join(', ')}
          </div>
          {picked.status !== 'CLASS' && (
            <div className={picked.status === 'CANCELLED' ? 'bad' : 'good'}>
              {picked.status === 'CANCELLED' ? 'Cancelled' : 'Extra class'}
              {picked.note ? `: ${picked.note}` : ''}
            </div>
          )}
        </div>
      )}
    </div>
  )
}
