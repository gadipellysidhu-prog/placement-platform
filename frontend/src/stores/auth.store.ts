import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { User } from '@/types/auth'

interface AuthState {
  /** JWT access token — kept in sessionStorage, never persisted to localStorage. */
  accessToken: string | null
  /** Authenticated user object (email + role). */
  user: User | null
  isAuthenticated: boolean

  setAuth: (accessToken: string, refreshToken: string, user: User) => void
  setAccessToken: (token: string) => void
  clearAuth: () => void
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      accessToken: null,
      user: null,
      isAuthenticated: false,

      setAuth: (accessToken, refreshToken, user) => {
        sessionStorage.setItem('__access_token__', accessToken)
        localStorage.setItem('__refresh_token__', refreshToken)
        set({ accessToken, user, isAuthenticated: true })
      },

      setAccessToken: (token) => {
        sessionStorage.setItem('__access_token__', token)
        set({ accessToken: token })
      },

      clearAuth: () => {
        sessionStorage.removeItem('__access_token__')
        localStorage.removeItem('__refresh_token__')
        set({ accessToken: null, user: null, isAuthenticated: false })
      },
    }),
    {
      name: 'placement-auth',
      // Persist user profile so the UI can render before the /api/users/me call resolves
      // Never persist the access token — it lives in sessionStorage only
      partialize: (state) => ({
        user: state.user,
        isAuthenticated: state.isAuthenticated,
      }),
    },
  ),
)
