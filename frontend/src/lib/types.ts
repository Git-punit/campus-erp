// Shapes returned by the backend. Kept in one place so pages agree on them.

export interface Ref {
  id: number
  code: string
  name: string
}

export interface Department {
  id: number
  code: string
  name: string
}

export interface Programme {
  id: number
  code: string
  name: string
  department: Ref
  semesters: number
}

export interface Section {
  id: number
  code: string
  programme: Ref
}

export interface AcademicYear {
  id: number
  name: string
  oddSemStart: string
  oddSemEnd: string
  evenSemStart: string
  evenSemEnd: string
  current: boolean
}

export interface Holiday {
  id: number
  date: string
  name: string
}

export type FacultyStatus = 'ACTIVE' | 'ON_LEAVE' | 'LEFT'

export interface Faculty {
  id: number
  employeeId: string
  name: string
  department: Ref
  designation: string | null
  email: string | null
  phone: string | null
  status: FacultyStatus
  courseCount: number
  projectCount: number
}

export interface Course {
  id: number
  code: string
  name: string
  credits: number
  department: Ref
}

export type TeachingRole = 'THEORY' | 'LAB' | 'TUTORIAL'
export type Day = 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY'

export interface OfferingTeacher {
  facultyId: number
  employeeId: string
  name: string
  role: TeachingRole
}

export interface Slot {
  id?: number
  day: Day
  startTime: string
  endTime: string
  room: string
  type: TeachingRole
}

export interface Offering {
  id: number
  course: Ref
  credits: number
  academicYear: Ref
  semester: number
  section: Ref
  programme: Ref
  teachers: OfferingTeacher[]
  slots: Slot[]
  enrolled: number
}

export interface ClassChange {
  id: number
  offeringId: number
  date: string
  type: 'CANCELLED' | 'EXTRA'
  startTime: string
  endTime: string | null
  room: string | null
  reason: string | null
}

export type StudentStatus = 'ACTIVE' | 'ON_BREAK' | 'DROPPED' | 'GRADUATED'

export interface Student {
  id: number
  rollNo: string
  name: string
  programme: Ref
  currentSemester: number
  section: Ref
  email: string | null
  phone: string | null
  status: StudentStatus
}

export interface CalendarEntry {
  date: string
  startTime: string
  endTime: string
  offeringId: number
  course: Ref
  section: Ref
  semester: number
  room: string
  teachers: string[]
  status: 'CLASS' | 'CANCELLED' | 'EXTRA'
  note: string | null
}

export interface CalendarData {
  from: string
  to: string
  entries: CalendarEntry[]
  holidays: { date: string; name: string }[]
}

export type BudgetType = 'DEPARTMENT' | 'EVENT' | 'TRAINING' | 'PROJECT'

export interface BudgetSummary {
  id: number
  type: BudgetType
  name: string
  financialYear: string
  department: Ref | null
  source: { kind: string; id: number; name: string } | null
  allocated: number
  utilized: number
  pending: number
  balance: number
  percentUsed: number
  overspent: boolean
  pendingWouldOverspend: boolean
}

export type ExpenseStatus = 'PENDING' | 'APPROVED' | 'REJECTED'
export type ExpenseCategory = 'EQUIPMENT' | 'TRAVEL' | 'CONSUMABLES' | 'HONORARIUM' | 'OTHER'

export interface Expense {
  id: number
  budgetId: number
  budgetName: string
  budgetType: BudgetType
  date: string
  amount: number
  category: ExpenseCategory
  description: string
  status: ExpenseStatus
  raisedBy: string
  decidedBy: string | null
  decidedAt: string | null
  decisionRemark: string | null
  receiptName: string | null
  receiptSize: number | null
}

export type ProjectStatus = 'ONGOING' | 'COMPLETED' | 'CLOSED'
export type MemberRole = 'PI' | 'CO_INVESTIGATOR'

export interface Project {
  id: number
  title: string
  fundingAgency: string
  sanctionedAmount: number
  sanctionDate: string
  startDate: string | null
  endDate: string | null
  status: ProjectStatus
  team: { facultyId: number; employeeId: string; name: string; role: MemberRole }[]
  budgetId: number | null
  utilized: number
  pending: number
  balance: number
  percentUsed: number
}

export type Acceptance = 'YES' | 'NO' | 'NEEDS_REVISION'

export interface TrainingProgram {
  id: number
  name: string
  description: string | null
  durationDays: number | null
  coordinator: Ref | null
  clientsReached: number
  proposals: number
  accepted: number
  conducted: number
  totalFee: number
  totalCost: number
}

export interface TrainingClient {
  id: number
  programId: number
  programName: string
  clientName: string
  contactPerson: string | null
  phone: string | null
  email: string | null
  dateContacted: string | null
  proposalSubmitted: boolean
  proposalDate: string | null
  accepted: Acceptance | null
  acceptedDate: string | null
  acceptedRemarks: string | null
  conducted: boolean
  conductedFrom: string | null
  conductedTo: string | null
  budgetRequired: number | null
  feeCharged: number | null
  budgetId: number | null
  actualCost: number
  profit: number | null
  history: { date: string; accepted: Acceptance; remarks: string | null; changedBy: string | null }[]
}

export type EventStatus = 'PLANNED' | 'ONGOING' | 'COMPLETED' | 'CANCELLED'

export interface CampusEvent {
  id: number
  name: string
  type: string
  startDate: string
  endDate: string
  venue: string | null
  status: EventStatus
  budget: BudgetSummary | null
}
