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

const SERVER_DOWN =
  "Can't reach the server. Check that the backend is running (./mvnw spring-boot:run) and didn't stop with an error."

async function request<T>(method: string, path: string, body?: unknown, contentType?: string): Promise<T> {
  const headers: Record<string, string> = {}
  // FormData (file uploads) sets its own Content-Type; text bodies (CSV) are sent as-is
  const raw = body instanceof FormData || contentType !== undefined
  if (body !== undefined && !(body instanceof FormData)) headers['Content-Type'] = contentType ?? 'application/json'
  const token = getToken()
  if (token) headers['Authorization'] = `Bearer ${token}`

  let res: Response
  try {
    res = await fetch(`/api${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : raw ? (body as BodyInit) : JSON.stringify(body),
    })
  } catch {
    throw new ApiError(0, SERVER_DOWN)
  }

  // The dev server answers 502/503/504 itself when the backend on port 8080 isn't up
  if (res.status === 502 || res.status === 503 || res.status === 504) {
    throw new ApiError(res.status, SERVER_DOWN)
  }

  // Token expired or user disabled: drop the token and let the app show the login page
  if (res.status === 401 && path !== '/auth/login' && token) {
    setToken(null)
    onUnauthorized?.()
  }

  if (res.status === 204) return undefined as T

  const text = await res.text()
  let data
  try {
    data = text ? JSON.parse(text) : undefined
  } catch {
    // not JSON (e.g. an HTML error page): fall back to a generic message below
    data = undefined
  }

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
  /** Send text such as a CSV file, e.g. api.postText('/students/import', text) */
  postText: <T>(path: string, text: string, contentType = 'text/csv') => request<T>('POST', path, text, contentType),
  /** Upload one file as multipart form field "file" */
  upload: <T>(path: string, file: File) => {
    const form = new FormData()
    form.append('file', file)
    return request<T>('POST', path, form)
  },
}

/** Fetch a file that needs the login token (e.g. a receipt) and open it in a new tab. */
export async function openFile(path: string) {
  const token = getToken()
  const res = await fetch(`/api${path}`, { headers: token ? { Authorization: `Bearer ${token}` } : {} })
  if (!res.ok) throw new ApiError(res.status, `Could not open the file (${res.status})`)
  const url = URL.createObjectURL(await res.blob())
  const link = document.createElement('a')
  link.href = url
  link.target = '_blank'
  link.rel = 'noopener'
  link.click()
  setTimeout(() => URL.revokeObjectURL(url), 60_000)
}
