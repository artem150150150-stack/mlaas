import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import api from '../api/client'
import { useAuthStore } from '../store/authStore'
import type { AuthResponse } from '../types'

export function LoginPage() {
  const nav = useNavigate()
  const setAuth = useAuthStore((s) => s.setAuth)
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [err, setErr] = useState<string | null>(null)

  async function submit(e: FormEvent) {
    e.preventDefault()
    setErr(null)
    try {
      const { data } = await api.post<AuthResponse>('/auth/login', { email, password })
      setAuth(data.accessToken, data.refreshToken, data.user)
      nav('/')
    } catch {
      setErr('Неверный email или пароль')
    }
  }

  return (
    <div className="mx-auto flex min-h-screen max-w-md flex-col justify-center px-4">
      <h1 className="mb-6 text-2xl font-bold text-cyan-400">Вход в LumenML</h1>
      <form onSubmit={submit} className="flex flex-col gap-4">
        <input
          className="rounded-lg border border-slate-700 bg-slate-900 px-3 py-2"
          placeholder="Email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          type="email"
          required
        />
        <input
          className="rounded-lg border border-slate-700 bg-slate-900 px-3 py-2"
          placeholder="Пароль"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          type="password"
          required
        />
        {err ? <p className="text-sm text-red-400">{err}</p> : null}
        <button type="submit" className="rounded-lg bg-cyan-600 py-2 font-medium text-white hover:bg-cyan-500">
          Войти
        </button>
      </form>
      <p className="mt-4 text-sm text-slate-500">
        Нет аккаунта? <Link className="text-cyan-400 hover:underline" to="/register">Регистрация</Link>
      </p>
    </div>
  )
}
