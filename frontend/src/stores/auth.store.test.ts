import { beforeEach, describe, expect, it } from 'vitest'
import { useAuthStore } from './auth.store'
import type { User } from '@/types/auth'

const user: User = { email: 'student@university.edu', role: 'ROLE_STUDENT' }

function resetStore() {
  useAuthStore.setState({ accessToken: null, user: null, isAuthenticated: false })
  sessionStorage.clear()
  localStorage.clear()
}

describe('auth.store', () => {
  beforeEach(resetStore)

  it('starts unauthenticated with no tokens', () => {
    const state = useAuthStore.getState()
    expect(state.isAuthenticated).toBe(false)
    expect(state.user).toBeNull()
    expect(state.accessToken).toBeNull()
  })

  it('setAuth authenticates the user and writes tokens to the correct storages', () => {
    useAuthStore.getState().setAuth('access-1', 'refresh-1', user)

    const state = useAuthStore.getState()
    expect(state.isAuthenticated).toBe(true)
    expect(state.user).toEqual(user)
    expect(state.accessToken).toBe('access-1')

    // Access token lives in sessionStorage; refresh token in localStorage.
    expect(sessionStorage.getItem('__access_token__')).toBe('access-1')
    expect(localStorage.getItem('__refresh_token__')).toBe('refresh-1')
  })

  it('setAccessToken rotates only the access token, leaving user/refresh intact', () => {
    useAuthStore.getState().setAuth('access-1', 'refresh-1', user)
    useAuthStore.getState().setAccessToken('access-2')

    const state = useAuthStore.getState()
    expect(state.accessToken).toBe('access-2')
    expect(state.user).toEqual(user)
    expect(sessionStorage.getItem('__access_token__')).toBe('access-2')
    // Refresh token untouched by an access-token rotation.
    expect(localStorage.getItem('__refresh_token__')).toBe('refresh-1')
  })

  it('clearAuth resets state and removes both tokens', () => {
    useAuthStore.getState().setAuth('access-1', 'refresh-1', user)
    useAuthStore.getState().clearAuth()

    const state = useAuthStore.getState()
    expect(state.isAuthenticated).toBe(false)
    expect(state.user).toBeNull()
    expect(state.accessToken).toBeNull()
    expect(sessionStorage.getItem('__access_token__')).toBeNull()
    expect(localStorage.getItem('__refresh_token__')).toBeNull()
  })

  it('persists user profile but never the access token to localStorage', () => {
    useAuthStore.getState().setAuth('access-1', 'refresh-1', user)

    const persisted = localStorage.getItem('placement-auth')
    expect(persisted).not.toBeNull()

    const parsed = JSON.parse(persisted as string) as {
      state: Record<string, unknown>
    }
    expect(parsed.state.user).toEqual(user)
    expect(parsed.state.isAuthenticated).toBe(true)
    // The access token must not leak into the persisted (localStorage) partition.
    expect(parsed.state.accessToken).toBeUndefined()
  })
})
