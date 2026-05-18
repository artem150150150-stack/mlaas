import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import api from '../api/client'
import { Card } from '../components/Card'
import type { ProjectDto } from '../types'

interface Page<T> {
  content: T[]
}

export function ProjectsPage() {
  const qc = useQueryClient()
  const [name, setName] = useState('')
  const { data } = useQuery({
    queryKey: ['projects'],
    queryFn: async () => {
      const { data } = await api.get<Page<ProjectDto>>('/projects', { params: { size: 50 } })
      return data
    },
  })

  const create = useMutation({
    mutationFn: async () => {
      await api.post('/projects', { name, description: '' })
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['projects'] })
      setName('')
    },
  })

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">Проекты</h1>
      <Card title="Новый проект">
        <div className="flex gap-2">
          <input
            className="flex-1 rounded-lg border border-slate-700 bg-slate-900 px-3 py-2"
            placeholder="Название"
            value={name}
            onChange={(e) => setName(e.target.value)}
          />
          <button
            type="button"
            disabled={!name.trim() || create.isPending}
            className="rounded-lg bg-cyan-600 px-4 py-2 font-medium disabled:opacity-50"
            onClick={() => create.mutate()}
          >
            Создать
          </button>
        </div>
      </Card>
      <Card title="Список">
        <ul className="divide-y divide-slate-800">
          {data?.content?.map((p) => (
            <li key={p.id} className="py-3">
              <Link className="text-lg text-cyan-400 hover:underline" to={`/projects/${p.id}`}>
                {p.name}
              </Link>
              <p className="text-sm text-slate-500">{p.description || '—'}</p>
            </li>
          ))}
        </ul>
      </Card>
    </div>
  )
}
