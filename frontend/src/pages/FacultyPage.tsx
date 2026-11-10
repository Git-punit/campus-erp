import { useState } from 'react'
import { Link } from 'react-router'
import { useAuth } from '../auth'
import ImportBox from '../components/ImportBox'
import CrudSection, { enumOptions, optionsOf, str, type CrudConfig } from '../components/CrudSection'
import { Badge } from '../components/ui'
import type { Department, Faculty } from '../lib/types'
import { useLoad } from '../lib/useLoad'

export const FACULTY_STATUSES = ['ACTIVE', 'ON_LEAVE', 'LEFT']

export default function FacultyPage() {
  const departments = useLoad<Department[]>('/departments')
  const { canEdit } = useAuth()
  const [showImport, setShowImport] = useState(false)
  const [version, setVersion] = useState(0) // bump to reload the list after an import

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
    csv: {
      fileName: 'faculty',
      row: (f) => ({
        employee_id: f.employeeId,
        name: f.name,
        department: f.department.code,
        designation: f.designation,
        email: f.email,
        phone: f.phone,
        status: f.status,
        courses_this_year: f.courseCount,
        projects: f.projectCount,
      }),
    },
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

  const importButton = canEdit ? (
    <button className="btn" type="button" onClick={() => setShowImport(!showImport)}>
      Import CSV
    </button>
  ) : null

  return (
    <>
      <h1>Faculty</h1>
      <p className="muted">Courses are counted for the current academic year. Click a count to see the list behind it.</p>
      {showImport && (
        <ImportBox
          what="faculty"
          endpoint="/faculty/import"
          required={['employee_id', 'name', 'department']}
          optional={['designation', 'email', 'phone', 'status']}
          example={{ employee_id: 'FAC101', name: 'Dr. Meera Iyer', department: 'CSE', designation: 'Assistant Professor', email: 'meera@example.edu', status: 'ACTIVE' }}
          onDone={() => setVersion((v) => v + 1)}
          onClose={() => setShowImport(false)}
        />
      )}
      <CrudSection config={{ ...config, headerExtra: importButton }} key={version} />
    </>
  )
}
