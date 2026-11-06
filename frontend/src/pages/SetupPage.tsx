import CrudSection, { optionsOf, str, type CrudConfig } from '../components/CrudSection'
import { Tabs } from '../components/ui'
import { date } from '../lib/format'
import type { AcademicYear, Department, Holiday, Programme, Section } from '../lib/types'
import { useLoad } from '../lib/useLoad'

export default function SetupPage() {
  const departments = useLoad<Department[]>('/departments')
  const programmes = useLoad<Programme[]>('/programmes')

  const departmentConfig: CrudConfig<Department> = {
    noun: 'department',
    listPath: '/departments',
    savePath: '/departments',
    columns: [
      { label: 'Code', render: (d) => d.code },
      { label: 'Name', render: (d) => d.name },
    ],
    fields: [
      { name: 'code', label: 'Code', required: true, placeholder: 'CSE' },
      { name: 'name', label: 'Name', required: true, wide: true },
    ],
    empty: { code: '', name: '' },
    toForm: (d) => ({ code: d.code, name: d.name }),
  }

  const programmeConfig: CrudConfig<Programme> = {
    noun: 'programme',
    listPath: '/programmes',
    savePath: '/programmes',
    columns: [
      { label: 'Code', render: (p) => p.code },
      { label: 'Name', render: (p) => p.name },
      { label: 'Department', render: (p) => p.department.code },
      { label: 'Semesters', render: (p) => p.semesters },
    ],
    fields: [
      { name: 'code', label: 'Code', required: true, placeholder: 'BTECH-CSE' },
      { name: 'name', label: 'Name', required: true, wide: true },
      { name: 'departmentId', label: 'Department', type: 'select', required: true, options: optionsOf(departments.data, (d) => d.code + ' - ' + d.name) },
      { name: 'semesters', label: 'Number of semesters', type: 'number', required: true },
    ],
    empty: { code: '', name: '', departmentId: '', semesters: '8' },
    toForm: (p) => ({ code: p.code, name: p.name, departmentId: str(p.department.id), semesters: str(p.semesters) }),
  }

  const sectionConfig: CrudConfig<Section> = {
    noun: 'section',
    listPath: '/sections',
    savePath: '/sections',
    columns: [
      { label: 'Section', render: (s) => s.code },
      { label: 'Programme', render: (s) => s.programme.name },
    ],
    fields: [
      { name: 'code', label: 'Section', required: true, placeholder: 'CSE-A' },
      { name: 'programmeId', label: 'Programme', type: 'select', required: true, options: optionsOf(programmes.data, (p) => p.name) },
    ],
    empty: { code: '', programmeId: '' },
    toForm: (s) => ({ code: s.code, programmeId: str(s.programme.id) }),
  }

  const yearConfig: CrudConfig<AcademicYear> = {
    noun: 'academic year',
    listPath: '/academic-years',
    savePath: '/academic-years',
    columns: [
      { label: 'Year', render: (y) => (y.current ? <><strong>{y.name}</strong> <span className="badge tone-active">current</span></> : y.name) },
      { label: 'Odd semesters', render: (y) => `${date(y.oddSemStart)} to ${date(y.oddSemEnd)}` },
      { label: 'Even semesters', render: (y) => `${date(y.evenSemStart)} to ${date(y.evenSemEnd)}` },
    ],
    fields: [
      { name: 'name', label: 'Year', required: true, placeholder: '2027-28' },
      { name: 'oddSemStart', label: 'Odd semester starts', type: 'date', required: true },
      { name: 'oddSemEnd', label: 'Odd semester ends', type: 'date', required: true },
      { name: 'evenSemStart', label: 'Even semester starts', type: 'date', required: true },
      { name: 'evenSemEnd', label: 'Even semester ends', type: 'date', required: true },
    ],
    empty: { name: '', oddSemStart: '', oddSemEnd: '', evenSemStart: '', evenSemEnd: '' },
    toForm: (y) => ({ name: y.name, oddSemStart: y.oddSemStart, oddSemEnd: y.oddSemEnd, evenSemStart: y.evenSemStart, evenSemEnd: y.evenSemEnd }),
  }

  const holidayConfig: CrudConfig<Holiday> = {
    noun: 'holiday',
    listPath: '/holidays',
    savePath: '/holidays',
    columns: [
      { label: 'Date', render: (h) => date(h.date) },
      { label: 'Day', render: (h) => new Date(h.date + 'T00:00').toLocaleDateString('en-IN', { weekday: 'long' }) },
      { label: 'Holiday', render: (h) => h.name },
    ],
    fields: [
      { name: 'date', label: 'Date', type: 'date', required: true },
      { name: 'name', label: 'Name', required: true, wide: true },
    ],
    empty: { date: '', name: '' },
    toForm: (h) => ({ date: h.date, name: h.name }),
    emptyText: 'No holidays yet. The calendar skips any date listed here.',
  }

  return (
    <>
      <h1>Academic setup</h1>
      <Tabs
        tabs={[
          { key: 'departments', label: 'Departments', content: () => <CrudSection config={{ ...departmentConfig }} key="d" /> },
          { key: 'programmes', label: 'Programmes', content: () => <CrudSection config={programmeConfig} key="p" /> },
          { key: 'sections', label: 'Sections', content: () => <CrudSection config={sectionConfig} key="s" /> },
          { key: 'years', label: 'Academic years', content: () => <CrudSection config={yearConfig} key="y" /> },
          { key: 'holidays', label: 'Holidays', content: () => <CrudSection config={holidayConfig} key="h" /> },
        ]}
      />
    </>
  )
}
