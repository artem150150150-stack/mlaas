import axios from 'axios'
import { useAuthStore } from '../store/authStore'
import type { AuthResponse } from '../types'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE ?? '/api/v1',
  headers: { 'Content-Type': 'application/json' },
})

api.interceptors.request.use((config) => {
  const t = useAuthStore.getState().accessToken
  if (t) config.headers.Authorization = `Bearer ${t}`
  return config
})

let refreshing: Promise<string | null> | null = null

async function refreshAccess(): Promise<string | null> {
  const { refreshToken, setAuth, logout, user } = useAuthStore.getState()
  if (!refreshToken || !user) {
    logout()
    return null
  }
  const { data } = await axios.post<AuthResponse>(
    `${import.meta.env.VITE_API_BASE ?? '/api/v1'}/auth/refresh`,
    { refreshToken },
  )
  setAuth(data.accessToken, data.refreshToken, data.user)
  return data.accessToken
}

api.interceptors.response.use(
  (r) => r,
  async (error) => {
    const original = error.config
    const url = String(original?.url ?? '')
    if (url.includes('/auth/refresh') || url.includes('/auth/login')) {
      return Promise.reject(error)
    }
    if (error.response?.status === 401 && !original._retry) {
      original._retry = true
      refreshing ??= refreshAccess().finally(() => {
        refreshing = null
      })
      const token = await refreshing
      if (token) {
        original.headers.Authorization = `Bearer ${token}`
        return api(original)
      }
    }
    return Promise.reject(error)
  },
)

export default api
