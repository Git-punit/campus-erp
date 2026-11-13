import { useEffect, useRef, useState, type KeyboardEvent } from 'react'
import { Link, useNavigate } from 'react-router'
import { api } from '../api'

interface Hit {
  type: string
  title: string
  detail: string | null
  link: string
}

/** The search box in the top bar: faculty, students, courses, projects, events and training programs. */
export default function SearchBox() {
  const [q, setQ] = useState('')
  const [hits, setHits] = useState<Hit[] | null>(null) // null while waiting for the server
  const [open, setOpen] = useState(false)
  const box = useRef<HTMLDivElement>(null)
  const navigate = useNavigate()

  // ask the server a moment after typing stops; ignore answers to older text
  useEffect(() => {
    const text = q.trim()
    setHits(null)
    if (text.length < 2) return
    let stale = false
    const timer = setTimeout(() => {
      api
        .get<Hit[]>(`/search?q=${encodeURIComponent(text)}`)
        .then((h) => !stale && setHits(h))
        .catch(() => !stale && setHits([]))
    }, 250)
    return () => {
      stale = true
      clearTimeout(timer)
    }
  }, [q])

  // opening a result clears the box (also when the result is the page already open)
  function done() {
    setQ('')
    setOpen(false)
  }

  // clicking anywhere else closes the results
  useEffect(() => {
    if (!open) return
    const onClick = (e: MouseEvent) => {
      if (box.current && !box.current.contains(e.target as Node)) setOpen(false)
    }
    document.addEventListener('mousedown', onClick)
    return () => document.removeEventListener('mousedown', onClick)
  }, [open])

  function onKey(e: KeyboardEvent<HTMLInputElement>) {
    if (e.key === 'Enter' && hits && hits.length > 0) {
      navigate(hits[0].link)
      done()
    }
    if (e.key === 'Escape') setOpen(false)
  }

  const showList = open && q.trim().length >= 2

  return (
    <div className="search" ref={box} role="search">
      <svg className="search-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
        strokeWidth="2" strokeLinecap="round" aria-hidden="true">
        <circle cx="11" cy="11" r="7" />
        <path d="M20 20l-3.5-3.5" />
      </svg>
      <input
        type="search"
        value={q}
        placeholder="Search faculty, students, courses..."
        aria-label="Search"
        onChange={(e) => {
          setQ(e.target.value)
          setOpen(true)
        }}
        onFocus={() => setOpen(true)}
        onKeyDown={onKey}
      />
      {showList && (
        <div className="search-results">
          {hits === null ? (
            <p className="muted small">Searching...</p>
          ) : hits.length === 0 ? (
            <p className="muted small">No matches</p>
          ) : (
            hits.map((h, i) => (
              <Link key={i} to={h.link} onClick={done}>
                <span className="search-type">{h.type}</span>
                <span>{h.title}</span>
                {h.detail && <span className="muted small"> {h.detail}</span>}
              </Link>
            ))
          )}
        </div>
      )}
    </div>
  )
}
