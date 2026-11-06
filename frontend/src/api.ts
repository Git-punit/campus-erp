// Small wrapper around fetch for talking to the Spring Boot API.
// All paths are relative to /api, e.g. api.get('/faculty').

const TOKEN_KEY = 'campus-erp-token'

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string | null) {
  if (token) localStorage.setItem(TOKEN_KEY, token)
  else localStorage.removeItem(TOKEN_KEY)
}

let onUnauthorized: (() => void) | null = null

/** Called when the server rejects our token (see auth.tsx). */
export function setUnauthorizedHandler(fn: (() => void) | null) {
  onUnauthorized = fn
}

// Matches com.campuserp.common.ApiError on the backend
export class ApiError extends Error {
  status: number
  fieldErrors: Record<string, string>

  constructor(status: number, message: string, fieldErrors: Record<string, string> = {}) {
    super(message)
    this.status = status
    this.fieldErrors = fieldErrors
  }
}

async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
  const headers: Record<string, string> = {}
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  const token = getToken()
  if (token) headers['Authorization'] = `Bearer ${token}`

  let res: Response
  try {
    res = await fetch(`/api${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    })
  } catch {
    throw new ApiError(0, 'Could not reach the server')
  }

  // Token expired or user disabled: drop the token and let the app show the login page
  if (res.status === 401 && path !== '/auth/login' && token) {
    setToken(null)
    onUnauthorized?.()
  }

  if (res.status === 204) return undefined as T

  const text = await res.text()
  const data = text ? JSON.parse(text) : undefined

  if (!res.ok) {
    const message = data?.message ?? `Request failed (${res.status})`
    throw new ApiError(res.status, message, data?.fieldErrors ?? {})
  }
  return data as T
}

export const api = {
  get: <T>(path: string) => request<T>('GET', path),
  post: <T>(path: string, body?: unknown) => request<T>('POST', path, body),
  put: <T>(path: string, body?: unknown) => request<T>('PUT', path, body),
  delete: <T = void>(path: string) => request<T>('DELETE', path),
}
