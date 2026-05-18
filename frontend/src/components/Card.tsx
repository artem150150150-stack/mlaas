import type { ReactNode } from 'react'

export function Card({
  title,
  children,
  className = '',
}: {
  title?: string
  children: ReactNode
  className?: string
}) {
  return (
    <div
      className={`rounded-xl border border-slate-800 bg-slate-900/60 p-5 shadow-lg shadow-cyan-950/20 ${className}`}
    >
      {title ? <h2 className="mb-3 text-lg font-semibold text-cyan-300">{title}</h2> : null}
      {children}
    </div>
  )
}
