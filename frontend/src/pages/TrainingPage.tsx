import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import api from '../api/client'
import { Card } from '../components/Card'
import type { DatasetDto, ModelKind, TrainingTaskDto } from '../types'

export function TrainingPage() {
  const { id } = useParams<{ id: string }>()
  const nav = useNavigate()
  const qc = useQueryClient()
  const [datasetId, setDatasetId] = useState('')
  const [modelKind, setModelKind] = useState<ModelKind>('RANDOM_FOREST')

  const { data: datasets } = useQuery({
    queryKey: ['datasets', id],
    queryFn: async () => (await api.get<DatasetDto[]>(`/projects/${id}/datasets`)).data,
    enabled: !!id,
  })

  const start = useMutation({
    mutationFn: async () => {
      const { data } = await api.post<TrainingTaskDto>(`/projects/${id}/training`, {
        datasetId,
        modelKind,
        hyperparameters: { n_estimators: 150, max_depth: 12 },
      })
      return data
    },
    onSuccess: (task) => {
      qc.invalidateQueries({ queryKey: ['tasks', id] })
      nav(`/tasks/${task.id}/explain`)
    },
  })

  return (
    <div className="mx-auto max-w-xl space-y-6">
      <h1 className="text-2xl font-bold">Запуск обучения</h1>
      <Card>
        <div className="flex flex-col gap-4">
          <label className="text-sm text-slate-400">
            Датасет
            <select
              className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-900 px-3 py-2"
              value={datasetId}
              onChange={(e) => setDatasetId(e.target.value)}
            >
              <option value="">— выберите —</option>
              {datasets?.map((d) => (
                <option key={d.id} value={d.id}>
                  {d.originalFilename} ({d.taskType})
                </option>
              ))}
            </select>
          </label>
          <label className="text-sm text-slate-400">
            Модель
            <select
              className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-900 px-3 py-2"
              value={modelKind}
              onChange={(e) => setModelKind(e.target.value as ModelKind)}
            >
              <option value="RANDOM_FOREST">Random Forest</option>
              <option value="XGBOOST">Gradient Boosting (XGBoost-подобно)</option>
              <option value="LOGISTIC_REGRESSION">Logistic Regression</option>
              <option value="LINEAR_REGRESSION">Linear Regression</option>
            </select>
          </label>
          {start.isError ? (
            <p className="text-sm text-red-400">Не удалось поставить задачу в очередь</p>
          ) : null}
          <div className="flex gap-2">
            <button
              type="button"
              className="rounded-lg bg-cyan-600 px-4 py-2 font-medium disabled:opacity-50"
              disabled={!datasetId || start.isPending}
              onClick={() => start.mutate()}
            >
              Обучить
            </button>
            <Link to={`/projects/${id}`} className="rounded-lg border border-slate-600 px-4 py-2 text-sm">
              Назад
            </Link>
          </div>
        </div>
      </Card>
    </div>
  )
}
