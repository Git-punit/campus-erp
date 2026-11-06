interface Props {
  title: string
  phase?: number
  message?: string
}

export default function PlaceholderPage({ title, phase, message }: Props) {
  const text = message ?? (phase ? `Not built yet (phase ${phase}).` : 'Nothing here.')
  return (
    <>
      <h1>{title}</h1>
      <p className="muted">{text}</p>
    </>
  )
}
