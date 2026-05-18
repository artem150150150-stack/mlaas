import { useQuery } from '@tanstack/react-query'
import { Link, useParams } from 'react-router-dom'
import api from '../api/client'
import { Card } from '../components/Card'
import type { DatasetDto, ProjectDto } from '../types'

export function ProjectDetailPage() {
  const { id } = useParams<{ id: string }>()
  const { data: project } = useQuery({
    queryKey: ['project', id],
    queryFn: async () => (await api.get<ProjectDto>(`/projects/${id}`)).data,
    enabled: !!id,
  })
  const { data: datasets } = useQuery({
    queryKey: ['datasets', id],
    queryFn: async () => (await api.get<DatasetDto[]>(`/projects/${id}/datasets`)).data,
    enabled: !!id,
  })

  if (!id) return null

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">{project?.name ?? '…'}</h1>
      <div className="flex gap-3">
        <Link
          to={`/projects/${id}/upload`}
          className="rounded-lg bg-cyan-700 px-4 py-2 text-sm font-medium hover:bg-cyan-600"
        >
          Загрузить датасет
        </Link>
        <Link
          to={`/projects/${id}/train`}
          className="rounded-lg border border-slate-600 px-4 py-2 text-sm hover:bg-slate-800"
        >
          Обучение
        </Link>
      </div>
      <Card title="Датасеты">
        <ul className="space-y-2">
          {datasets?.map((d) => (
            <li key={d.id} className="text-slate-300">
              <span className="font-mono text-xs text-slate-500">{d.id.slice(0, 8)}…</span>{' '}
              {d.originalFilename} — {d.taskType}, target: <code>{d.targetColumn}</code>, rows:{' '}
              {d.rowCount ?? '?'}
            </li>
          ))}
          {!datasets?.length ? <li className="text-slate-500">Нет загруженных CSV</li> : null}
        </ul>
      </Card>
    </div>
  )
}
