import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import api from '../api/client'
import { Card } from '../components/Card'
import type { DatasetDto, TaskType } from '../types'

export function DatasetUploadPage() {
  const { id } = useParams<{ id: string }>()
  const nav = useNavigate()
  const qc = useQueryClient()
  const [taskType, setTaskType] = useState<TaskType>('CLASSIFICATION')
  const [target, setTarget] = useState('')
  const [features, setFeatures] = useState('')
  const [file, setFile] = useState<File | null>(null)

  const upload = useMutation({
    mutationFn: async () => {
      if (!id || !file) throw new Error('file')
      const meta = {
        taskType,
        targetColumn: target.trim(),
        featureColumns: features.split(',').map((s) => s.trim()).filter(Boolean),
      }
      const fd = new FormData()
      fd.append('metadata', new Blob([JSON.stringify(meta)], { type: 'application/json' }))
      fd.append('file', file)
      await api.post<DatasetDto>(`/projects/${id}/datasets`, fd, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['datasets', id] })
      nav(`/projects/${id}`)
    },
  })

  return (
    <div className="mx-auto max-w-xl space-y-6">
      <h1 className="text-2xl font-bold">Загрузка CSV</h1>
      <Card>
        <div className="flex flex-col gap-4">
          <label className="text-sm text-slate-400">
            Тип задачи
            <select
              className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-900 px-3 py-2"
              value={taskType}
              onChange={(e) => setTaskType(e.target.value as TaskType)}
            >
              <option value="CLASSIFICATION">Классификация</option>
              <option value="REGRESSION">Регрессия</option>
            </select>
          </label>
          <label className="text-sm text-slate-400">
            Целевая колонка
            <input
              className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-900 px-3 py-2"
              value={target}
              onChange={(e) => setTarget(e.target.value)}
              placeholder="например species"
            />
          </label>
          <label className="text-sm text-slate-400">
            Признаки через запятую
            <input
              className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-900 px-3 py-2"
              value={features}
              onChange={(e) => setFeatures(e.target.value)}
              placeholder="sepal_length,sepal_width,petal_length,petal_width"
            />
          </label>
          <input type="file" accept=".csv" onChange={(e) => setFile(e.target.files?.[0] ?? null)} />
          {upload.isError ? (
            <p className="text-sm text-red-400">Ошибка загрузки — проверьте CSV и заголовки колонок</p>
          ) : null}
          <div className="flex gap-2">
            <button
              type="button"
              className="rounded-lg bg-cyan-600 px-4 py-2 font-medium disabled:opacity-50"
              disabled={upload.isPending || !file || !target || !features}
              onClick={() => upload.mutate()}
            >
              Загрузить
            </button>
            <Link to={`/projects/${id}`} className="rounded-lg border border-slate-600 px-4 py-2 text-sm">
              Отмена
            </Link>
          </div>
        </div>
      </Card>
    </div>
  )
}
