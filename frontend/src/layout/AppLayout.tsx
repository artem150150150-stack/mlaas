import { NavLink, Outlet } from 'react-router-dom'
import { useAuthStore } from '../store/authStore'

const navCls = ({ isActive }: { isActive: boolean }) =>
  `rounded-lg px-3 py-2 text-sm font-medium ${isActive ? 'bg-cyan-600 text-white' : 'text-slate-400 hover:bg-slate-800'}`

export function AppLayout() {
  const { user, logout } = useAuthStore()
  return (
    <div className="min-h-screen flex">
      <aside className="w-56 shrink-0 border-r border-slate-800 bg-slate-950 p-4">
        <div className="mb-8 font-bold tracking-tight text-cyan-400">LumenML</div>
        <nav className="flex flex-col gap-1">
          <NavLink to="/" end className={navCls}>
            Дашборд
          </NavLink>
          <NavLink to="/projects" className={navCls}>
            Проекты
          </NavLink>
          <NavLink to="/monitoring" className={navCls}>
            Мониторинг
          </NavLink>
        </nav>
        <div className="mt-8 text-xs text-slate-500">
          <div className="truncate">{user?.email}</div>
          <div className="text-slate-600">{user?.role}</div>
          <button
            type="button"
            className="mt-2 text-cyan-500 hover:underline"
            onClick={() => {
              logout()
              window.location.href = '/login'
            }}
          >
            Выйти
          </button>
        </div>
      </aside>
      <main className="flex-1 p-8">
        <Outlet />
      </main>
    </div>
  )
}
