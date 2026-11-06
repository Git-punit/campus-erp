import { useCallback, useEffect, useState } from 'react'
import { api } from '../api'

/** Loads a GET endpoint and gives back data, error and a reload function. Pass null to skip loading. */
export function useLoad<T>(path: string | null) {
  const [data, setData] = useState<T | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(path !== null)

  const reload = useCallback(() => {
    if (path === null) {
      setData(null)
      setLoading(false)
      return
    }
    setLoading(true)
    api
      .get<T>(path)
      .then((d) => {
        setData(d)
        setError(null)
      })
      .catch((e: Error) => setError(e.message))
      .finally(() => setLoading(false))
  }, [path])

  useEffect(reload, [reload])

  return { data, error, loading, reload, setData }
}
