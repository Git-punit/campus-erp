import { Link } from 'react-router'
import CrudSection, { enumOptions, optionsOf, str, type CrudConfig } from '../components/CrudSection'
import { Badge } from '../components/ui'
import type { Department, Faculty } from '../lib/types'
import { useLoad } from '../lib/useLoad'

export const FACULTY_STATUSES = ['ACTIVE', 'ON_LEAVE', 'LEFT']

export default function FacultyPage() {
  const departments = useLoad<Department[]>('/departments')

  const config: CrudConfig<Faculty> = {
    noun: 'teacher',
    listPath: '/faculty',
    savePath: '/faculty',
    columns: [
      { label: 'Name', render: (f) => <Link to={`/faculty/${f.id}`}>{f.name}</Link> },
      { label: 'ID', render: (f) => f.employeeId },
      { label: 'Department', render: (f) => f.department.code },
      { label: 'Designation', render: (f) => f.designation ?? '-' },
      {
        label: 'Courses',
        className: 'num',
        render: (f) => <Link to={`/faculty/${f.id}?tab=courses`} title="Courses this academic year">{f.courseCount}</Link>,
      },
      {
        label: 'Projects',
        className: 'num',
        render: (f) => <Link to={`/faculty/${f.id}?tab=projects`}>{f.projectCount}</Link>,
      },
      { label: 'Status', render: (f) => <Badge value={f.status} /> },
    ],
    fields: [
      { name: 'employeeId', label: 'Employee ID', required: true },
      { name: 'name', label: 'Name', required: true },
      { name: 'departmentId', label: 'Department', type: 'select', required: true, options: optionsOf(departments.data, (d) => d.code + ' - ' + d.name) },
      { name: 'designation', label: 'Designation' },
      { name: 'email', label: 'Email', type: 'email' },
      { name: 'phone', label: 'Phone' },
      { name: 'status', label: 'Status', type: 'select', required: true, options: enumOptions(FACULTY_STATUSES) },
    ],
    empty: { employeeId: '', name: '', departmentId: '', designation: '', email: '', phone: '', status: 'ACTIVE' },
    toForm: (f) => ({
      employeeId: f.employeeId,
      name: f.name,
      departmentId: str(f.department.id),
      designation: str(f.designation),
      email: str(f.email),
      phone: str(f.phone),
      status: f.status,
    }),
  }

  return (
    <>
      <h1>Faculty</h1>
      <p className="muted">Courses are counted for the current academic year. Click a count to see the list behind it.</p>
      <CrudSection config={config} />
    </>
  )
}
