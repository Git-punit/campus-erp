interface Props {
  title: string
  phase?: number
}

export default function PlaceholderPage({ title, phase }: Props) {
  return (
    <>
      <h1>{title}</h1>
      <p className="muted">{phase ? `Not built yet (phase ${phase}).` : 'Nothing here.'}</p>
    </>
  )
}
