import { useQuery } from '@tanstack/react-query'
import { useParams } from 'react-router-dom'
import {
  Bar,
  BarChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import api from '../api/client'
import { Card } from '../components/Card'
import type { RecommendationDto, TrainingTaskDto } from '../types'

interface ExplainDto {
  id: string
  trainingTaskId: string
  featureImportance: Record<string, number>
  shapValues: number[][] | null
  limeExplanations: unknown[] | null
  fairnessMetrics: unknown
  createdAt: string
}

export function ExplainPage() {
  const { taskId } = useParams<{ taskId: string }>()

  const taskQ = useQuery({
    queryKey: ['task', taskId],
    queryFn: async () => (await api.get<TrainingTaskDto>(`/training/tasks/${taskId}`)).data,
    enabled: !!taskId,
    refetchInterval: (q) => {
      const s = q.state.data?.status
      return s === 'QUEUED' || s === 'RUNNING' || s === 'PENDING' ? 2000 : false
    },
  })

  const explainQ = useQuery({
    queryKey: ['explain', taskId],
    queryFn: async () => (await api.get<ExplainDto>(`/training/tasks/${taskId}/explain`)).data,
    enabled: !!taskId && taskQ.data?.status === 'SUCCEEDED',
    retry: false,
  })

  const recoQ = useQuery({
    queryKey: ['reco', taskId],
    queryFn: async () => (await api.get<RecommendationDto[]>(`/training/tasks/${taskId}/recommendations`)).data,
    enabled: !!taskId && taskQ.data?.status === 'SUCCEEDED',
    retry: false,
  })

  const fi = explainQ.data?.featureImportance
  const chartData =
    fi &&
    Object.entries(fi)
      .map(([name, value]) => ({ name, value }))
      .sort((a, b) => b.value - a.value)
      .slice(0, 12)

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">Explainability</h1>
      <Card title="Статус обучения">
        <p>
          <span className="text-slate-400">Статус:</span>{' '}
          <span className="font-mono text-cyan-300">{taskQ.data?.status}</span>
        </p>
        {taskQ.data?.errorMessage ? (
          <p className="mt-2 text-sm text-red-400">{taskQ.data.errorMessage}</p>
        ) : null}
        {taskQ.data?.metrics ? (
          <pre className="mt-3 overflow-x-auto rounded bg-slate-950 p-3 text-xs">
            {JSON.stringify(taskQ.data.metrics, null, 2)}
          </pre>
        ) : null}
      </Card>
      {taskQ.data?.status === 'SUCCEEDED' && chartData?.length ? (
        <Card title="Feature importance">
          <div className="h-72 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={chartData} layout="vertical" margin={{ left: 80 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#334155" />
                <XAxis type="number" stroke="#94a3b8" />
                <YAxis type="category" dataKey="name" width={100} stroke="#94a3b8" />
                <Tooltip contentStyle={{ background: '#0f172a', border: '1px solid #334155' }} />
                <Bar dataKey="value" fill="#22d3ee" radius={[0, 4, 4, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </Card>
      ) : null}
      {recoQ.data?.length ? (
        <Card title="Рекомендации">
          <ul className="space-y-2">
            {recoQ.data.map((r) => (
              <li key={r.id} className="text-sm">
                <span className="text-amber-400">[{r.severity}]</span> {r.message}
              </li>
            ))}
          </ul>
        </Card>
      ) : null}
      {explainQ.isError ? (
        <p className="text-slate-500">Отчёт explainability появится после успешного обучения.</p>
      ) : null}
    </div>
  )
}
