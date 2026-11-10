// Turn rows into a CSV file and download it. Excel opens these directly.

type Cell = string | number | boolean | null | undefined

function cell(v: Cell): string {
  if (v === null || v === undefined) return ''
  const s = String(v)
  // quote anything with a comma, quote or line break; double inner quotes
  const quoted = /[",\r\n]/.test(s) ? `"${s.replace(/"/g, '""')}"` : s
  // stop spreadsheet apps treating a value like =SUM(...) as a formula
  return /^[=+\-@]/.test(s) && Number.isNaN(Number(s)) ? `'${quoted}` : quoted
}

export function toCsv(rows: Record<string, Cell>[]): string {
  if (rows.length === 0) return ''
  const header = Object.keys(rows[0])
  const lines = [header.map(cell).join(','), ...rows.map((r) => header.map((h) => cell(r[h])).join(','))]
  return lines.join('\r\n') + '\r\n'
}

export function downloadCsv(fileName: string, rows: Record<string, Cell>[]) {
  // the BOM makes Excel read the file as UTF-8 (names with accents, the rupee sign)
  const blob = new Blob(['﻿' + toCsv(rows)], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName.endsWith('.csv') ? fileName : `${fileName}.csv`
  link.click()
  setTimeout(() => URL.revokeObjectURL(url), 10_000)
}

export function ExportButton({ fileName, rows }: { fileName: string; rows: () => Record<string, Cell>[] }) {
  return (
    <button className="btn" type="button" onClick={() => downloadCsv(fileName, rows())}>
      Export CSV
    </button>
  )
}
