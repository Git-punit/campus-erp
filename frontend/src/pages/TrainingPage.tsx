import { Link } from 'react-router'
import CrudSection, { optionsOf, str, type CrudConfig } from '../components/CrudSection'
import { Money } from '../components/ui'
import type { Faculty, TrainingProgram } from '../lib/types'
import { useLoad } from '../lib/useLoad'

export default function TrainingPage() {
  const faculty = useLoad<Faculty[]>('/faculty')
  const config: CrudConfig<TrainingProgram> = {
    noun: 'program',
    listPath: '/training-programs',
    savePath: '/training-programs',
    columns: [
      { label: 'Program', render: (p) => <Link to={`/training/${p.id}`}>{p.name}</Link> },
      { label: 'Days', className: 'num', render: (p) => p.durationDays ?? '-' },
      { label: 'Coordinator', render: (p) => p.coordinator?.name ?? '-' },
      { label: 'Clients reached', className: 'num', render: (p) => p.clientsReached },
      { label: 'Proposals', className: 'num', render: (p) => p.proposals },
      { label: 'Accepted', className: 'num', render: (p) => p.accepted },
      { label: 'Conducted', className: 'num', render: (p) => p.conducted },
      { label: 'Fee vs cost', className: 'num', render: (p) => <><Money value={p.totalFee} /> / <Money value={p.totalCost} /></> },
    ],
    fields: [
      { name: 'name', label: 'Name', required: true, wide: true },
      { name: 'durationDays', label: 'Duration (days)', type: 'number' },
      { name: 'coordinatorId', label: 'Coordinator', type: 'select', options: optionsOf(faculty.data, (f) => `${f.name} (${f.department.code})`) },
      { name: 'description', label: 'Description', type: 'textarea', wide: true },
    ],
    empty: { name: '', durationDays: '', coordinatorId: '', description: '' },
    toForm: (p) => ({ name: p.name, durationDays: str(p.durationDays), coordinatorId: str(p.coordinator?.id), description: str(p.description) }),
  }
  return (
    <>
      <h1>Training programs</h1>
      <p className="muted">Programs offered to outside clients. Open a program to follow each client from first contact to delivery.</p>
      <CrudSection config={config} />
    </>
  )
}
