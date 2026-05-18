import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { UserDto } from '../types'

interface AuthState {
  accessToken: string | null
  refreshToken: string | null
  user: UserDto | null
  setAuth: (access: string, refresh: string, user: UserDto) => void
  setAccess: (token: string) => void
  logout: () => void
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      accessToken: null,
      refreshToken: null,
      user: null,
      setAuth: (access, refresh, user) =>
        set({ accessToken: access, refreshToken: refresh, user }),
      setAccess: (token) => set({ accessToken: token }),
      logout: () => set({ accessToken: null, refreshToken: null, user: null }),
    }),
    { name: 'lumenml-auth' },
  ),
)
