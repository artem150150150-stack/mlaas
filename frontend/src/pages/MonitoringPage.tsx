import { useQuery } from '@tanstack/react-query'
import api from '../api/client'
import { Card } from '../components/Card'

interface Dashboard {
  trainingQueue: { queue: string; messageCount: number | null; consumerCount: number | null }
  runningTasks: number
  hints: Record<string, unknown>
}

export function MonitoringPage() {
  const { data } = useQuery({
    queryKey: ['monitoring'],
    queryFn: async () => (await api.get<Dashboard>('/monitoring/dashboard')).data,
    refetchInterval: 5000,
  })

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">Мониторинг</h1>
      <div className="grid gap-4 md:grid-cols-2">
        <Card title="Очередь обучения">
          <p className="font-mono text-sm text-slate-300">{data?.trainingQueue.queue}</p>
          <p className="mt-2 text-slate-400">
            Сообщений: <span className="text-white">{data?.trainingQueue.messageCount ?? '—'}</span>
          </p>
          <p className="text-slate-400">
            Consumers: <span className="text-white">{data?.trainingQueue.consumerCount ?? '—'}</span>
          </p>
        </Card>
        <Card title="Активность">
          <p className="text-slate-400">
            Задач в статусе RUNNING:{' '}
            <span className="text-2xl font-bold text-cyan-400">{data?.runningTasks ?? 0}</span>
          </p>
        </Card>
      </div>
    </div>
  )
}
