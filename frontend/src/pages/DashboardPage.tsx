import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import api from '../api/client'
import { Card } from '../components/Card'
import type { ProjectDto } from '../types'

interface Page<T> {
  content: T[]
  totalElements: number
}

export function DashboardPage() {
  const { data: projects } = useQuery({
    queryKey: ['projects', 0],
    queryFn: async () => {
      const { data } = await api.get<Page<ProjectDto>>('/projects', { params: { page: 0, size: 5 } })
      return data
    },
  })

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">Дашборд</h1>
      <Card title="Последние проекты">
        <ul className="space-y-2">
          {projects?.content?.length ? (
            projects.content.map((p) => (
              <li key={p.id}>
                <Link className="text-cyan-400 hover:underline" to={`/projects/${p.id}`}>
                  {p.name}
                </Link>
              </li>
            ))
          ) : (
            <li className="text-slate-500">Пока нет проектов — создайте первый.</li>
          )}
        </ul>
        <Link
          to="/projects"
          className="mt-4 inline-block text-sm text-cyan-500 hover:underline"
        >
          Все проекты →
        </Link>
      </Card>
      <Card title="Быстрый старт">
        <ol className="list-decimal space-y-2 pl-5 text-slate-300">
          <li>Создайте ML-проект</li>
          <li>Загрузите CSV и укажите целевую колонку</li>
          <li>Запустите обучение — очередь RabbitMQ обработает задачу</li>
          <li>Откройте Explainability: SHAP, важность признаков, рекомендации</li>
        </ol>
      </Card>
    </div>
  )
}
