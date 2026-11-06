import { useEffect, useState } from 'react'
import { api } from '../api'

interface Health {
  status: string
  time: string
}

// For now this just proves the frontend can reach the backend.
// The real dashboard comes in phase 10.
export default function DashboardPage() {
  const [health, setHealth] = useState<Health | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    api
      .get<Health>('/health')
      .then(setHealth)
      .catch((e: Error) => setError(e.message))
  }, [])

  return (
    <>
      <h1>Dashboard</h1>
      <div className="card">
        <strong>Backend: </strong>
        {error && <span className="bad">not reachable ({error})</span>}
        {!error && !health && <span>checking...</span>}
        {health && (
          <span className="good">
            {health.status} (server time {new Date(health.time).toLocaleString()})
          </span>
        )}
      </div>
    </>
  )
}
