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

// a request with no answer after this long counts as failed (slow or dropped internet)
const TIMEOUT_MS = 60_000

/** The port of an address, with the default filled in (443 for https, 80 for http). */
const portOf = (url: URL) => url.port || (url.protocol === 'https:' ? '443' : '80')
const hostAndPort = (url: URL) => `${url.hostname}:${portOf(url)}`

/**
 * Why a request got no answer, naming the server and port it tried. In development that is the
 * backend behind the dev server (port 8080); on the live site it's the site itself.
 */
function cantReach(problem: string): string {
  if (!navigator.onLine) return "You're offline, so the data couldn't be loaded. Check your internet connection and try again."
  if (import.meta.env.DEV) {
    return `Can't reach the backend at ${hostAndPort(new URL(__BACKEND__))}: ${problem}. `
      + `This page runs on port ${portOf(new URL(location.href))} and forwards data requests there. `
      + 'Start the backend with ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev and check it didn\'t stop with an error.'
  }
  return `Can't reach the server at ${hostAndPort(new URL(location.href))}: ${problem}. `
    + 'Check your internet connection and try again in a minute.'
}

/** fetch with a time limit; turns network failures into a readable ApiError. */
async function send(url: string, init: RequestInit): Promise<Response> {
  let res: Response
  try {
    res = await fetch(url, { ...init, signal: AbortSignal.timeout(TIMEOUT_MS) })
  } catch (e) {
    const timedOut = e instanceof DOMException && e.name === 'TimeoutError'
    throw new ApiError(0, cantReach(timedOut ? `no answer after ${TIMEOUT_MS / 1000} seconds` : 'the connection failed'))
  }
  // 502/503/504 come from the dev server or the host when the backend behind it is down or restarting
  if (res.status === 502 || res.status === 503 || res.status === 504) {
    throw new ApiError(res.status, cantReach(`the server is down or restarting (HTTP ${res.status})`))
  }
  return res
}

async function request<T>(method: string, path: string, body?: unknown, contentType?: string): Promise<T> {
  const headers: Record<string, string> = {}
  // FormData (file uploads) sets its own Content-Type; text bodies (CSV) are sent as-is
  const raw = body instanceof FormData || contentType !== undefined
  if (body !== undefined && !(body instanceof FormData)) headers['Content-Type'] = contentType ?? 'application/json'
  const token = getToken()
  if (token) headers['Authorization'] = `Bearer ${token}`

  const res = await send(`/api${path}`, {
    method,
    headers,
    body: body === undefined ? undefined : raw ? (body as BodyInit) : JSON.stringify(body),
  })

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
  const res = await send(`/api${path}`, { headers: token ? { Authorization: `Bearer ${token}` } : {} })
  if (!res.ok) throw new ApiError(res.status, `Could not open the file (${res.status})`)
  const url = URL.createObjectURL(await res.blob())
  const link = document.createElement('a')
  link.href = url
  link.target = '_blank'
  link.rel = 'noopener'
  link.click()
  setTimeout(() => URL.revokeObjectURL(url), 60_000)
}
