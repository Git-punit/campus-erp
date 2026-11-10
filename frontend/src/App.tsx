import type { ReactNode } from 'react'
import { Navigate, Route, Routes, useLocation } from 'react-router'
import { useAuth } from './auth'
import Layout from './layout/Layout'
import ThemeToggle from './components/ThemeToggle'
import AccountPage from './pages/AccountPage'
import BudgetDetailPage from './pages/BudgetDetailPage'
import BudgetsPage from './pages/BudgetsPage'
import CalendarPage from './pages/CalendarPage'
import CoursesPage from './pages/CoursesPage'
import DashboardPage from './pages/DashboardPage'
import EventsPage from './pages/EventsPage'
import FacultyDetailPage from './pages/FacultyDetailPage'
import FacultyPage from './pages/FacultyPage'
import LoginPage from './pages/LoginPage'
import PlaceholderPage from './pages/PlaceholderPage'
import ProjectsPage from './pages/ProjectsPage'
import SetupPage from './pages/SetupPage'
import StudentDetailPage from './pages/StudentDetailPage'
import StudentsPage from './pages/StudentsPage'
import TrainingPage from './pages/TrainingPage'
import TrainingProgramPage from './pages/TrainingProgramPage'
import UsersPage from './pages/UsersPage'

function RequireLogin({ children }: { children: ReactNode }) {
  const { user, loading } = useAuth()
  const location = useLocation()
  if (loading) return <p className="muted center">Loading...</p>
  if (!user) return <Navigate to="/login" replace state={{ from: location.pathname + location.search }} />
  return children
}

function RequireAdmin({ children }: { children: ReactNode }) {
  const { isAdmin } = useAuth()
  if (!isAdmin) return <PlaceholderPage title="Not allowed" message="Only admins can open this page." />
  return children
}

export default function App() {
  return (
    <>
      <ThemeToggle />
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route
          element={
            <RequireLogin>
              <Layout />
            </RequireLogin>
          }
        >
          <Route index element={<Navigate to="/dashboard" replace />} />
          <Route path="dashboard" element={<DashboardPage />} />
          <Route path="account" element={<AccountPage />} />
          <Route path="faculty" element={<FacultyPage />} />
          <Route path="faculty/:id" element={<FacultyDetailPage />} />
          <Route path="courses" element={<CoursesPage />} />
          <Route path="students" element={<StudentsPage />} />
          <Route path="students/:id" element={<StudentDetailPage />} />
          <Route path="calendar" element={<CalendarPage />} />
          <Route path="budgets" element={<BudgetsPage />} />
          <Route path="budgets/:id" element={<BudgetDetailPage />} />
          <Route path="projects" element={<ProjectsPage />} />
          <Route path="training" element={<TrainingPage />} />
          <Route path="training/:id" element={<TrainingProgramPage />} />
          <Route path="events" element={<EventsPage />} />
          <Route path="setup" element={<SetupPage />} />
          <Route
            path="users"
            element={
              <RequireAdmin>
                <UsersPage />
              </RequireAdmin>
            }
          />
          <Route path="*" element={<PlaceholderPage title="Page not found" />} />
        </Route>
      </Routes>
    </>
  )
}
