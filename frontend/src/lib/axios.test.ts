import { afterAll, beforeAll, beforeEach, describe, expect, it, vi } from 'vitest'
import { http, HttpResponse } from 'msw'
// Import via the same '@/' alias the interceptor uses internally so the test and
// the axios module share a single store/axios instance (avoids duplicate modules).
import { apiClient } from '@/lib/axios'
import { useAuthStore } from '@/stores/auth.store'
import { server } from '@/test/msw/server'
import { API_BASE_URL } from '@/test/constants'

// Replace window.location so the interceptor's `window.location.href = '/login'`
// assignment does not attempt a (jsdom-unsupported) navigation.
const realLocation = window.location

beforeAll(() => {
  // A plain object (not a URL) so the interceptor's `href = '/login'` assignment
  // simply sets a property instead of triggering a jsdom navigation / URL parse.
  Object.defineProperty(window, 'location', {
    configurable: true,
    value: {
      href: 'http://localhost/',
      origin: 'http://localhost',
      pathname: '/',
      assign: () => {},
      replace: () => {},
      reload: () => {},
    } as unknown as Location,
  })
})

afterAll(() => {
  Object.defineProperty(window, 'location', { configurable: true, value: realLocation })
})

beforeEach(() => {
  useAuthStore.setState({ accessToken: null, user: null, isAuthenticated: false })
  sessionStorage.clear()
  localStorage.clear()
  // Reset the fake location: _handleAuthFailure sets href='/login', and a relative
  // href would break URL resolution (jsdom/axios) for requests in the next test.
  window.location.href = 'http://localhost/'
})

describe('axios apiClient interceptors', () => {
  it('request interceptor attaches the Bearer token from sessionStorage', async () => {
    sessionStorage.setItem('__access_token__', 'access-abc')

    let received: string | null = null
    server.use(
      http.get(`${API_BASE_URL}/api/ping`, ({ request }) => {
        received = request.headers.get('Authorization')
        return HttpResponse.json({ ok: true })
      }),
    )

    const res = await apiClient.get('/api/ping')
    expect(res.data).toEqual({ ok: true })
    expect(received).toBe('Bearer access-abc')
  })

  it('sends no Authorization header when there is no token', async () => {
    let received: string | null = 'sentinel'
    server.use(
      http.get(`${API_BASE_URL}/api/ping`, ({ request }) => {
        received = request.headers.get('Authorization')
        return HttpResponse.json({ ok: true })
      }),
    )

    await apiClient.get('/api/ping')
    expect(received).toBeNull()
  })

  it('on 401 it silently refreshes and retries the original request with the new token', async () => {
    sessionStorage.setItem('__access_token__', 'stale-token')
    localStorage.setItem('__refresh_token__', 'refresh-xyz')

    let refreshCalls = 0
    server.use(
      // Protected endpoint: 401 for the stale token, 200 once the fresh token arrives.
      http.get(`${API_BASE_URL}/api/protected`, ({ request }) => {
        const auth = request.headers.get('Authorization')
        if (auth === 'Bearer fresh-token') {
          return HttpResponse.json({ data: 'secret' })
        }
        return new HttpResponse(null, { status: 401 })
      }),
      http.post(`${API_BASE_URL}/auth/refresh`, async ({ request }) => {
        refreshCalls += 1
        const body = (await request.json()) as { refreshToken: string }
        expect(body.refreshToken).toBe('refresh-xyz')
        return HttpResponse.json({
          accessToken: 'fresh-token',
          refreshToken: 'refresh-new',
          accessTokenExpiresIn: 900,
          tokenType: 'Bearer',
        })
      }),
    )

    const res = await apiClient.get('/api/protected')

    expect(res.data).toEqual({ data: 'secret' })
    expect(refreshCalls).toBe(1)
    // New tokens persisted and reflected in the store.
    expect(sessionStorage.getItem('__access_token__')).toBe('fresh-token')
    expect(localStorage.getItem('__refresh_token__')).toBe('refresh-new')
    expect(useAuthStore.getState().accessToken).toBe('fresh-token')
  })

  it('on 401 with no refresh token it clears auth and rejects', async () => {
    useAuthStore.setState({ accessToken: 'stale', user: null, isAuthenticated: true })
    sessionStorage.setItem('__access_token__', 'stale')

    server.use(
      http.get(`${API_BASE_URL}/api/protected`, () => new HttpResponse(null, { status: 401 })),
    )

    await expect(apiClient.get('/api/protected')).rejects.toMatchObject({
      response: { status: 401 },
    })

    // clearAuth runs via an async dynamic import inside _handleAuthFailure.
    await vi.waitFor(() => expect(useAuthStore.getState().isAuthenticated).toBe(false))
    expect(sessionStorage.getItem('__access_token__')).toBeNull()
    expect(localStorage.getItem('__refresh_token__')).toBeNull()
  })

  it('when the refresh call itself fails it clears auth and rejects', async () => {
    useAuthStore.setState({ accessToken: 'stale', user: null, isAuthenticated: true })
    sessionStorage.setItem('__access_token__', 'stale')
    localStorage.setItem('__refresh_token__', 'refresh-expired')

    server.use(
      http.get(`${API_BASE_URL}/api/protected`, () => new HttpResponse(null, { status: 401 })),
      http.post(`${API_BASE_URL}/auth/refresh`, () => new HttpResponse(null, { status: 401 })),
    )

    await expect(apiClient.get('/api/protected')).rejects.toBeDefined()

    await vi.waitFor(() => expect(useAuthStore.getState().isAuthenticated).toBe(false))
    expect(sessionStorage.getItem('__access_token__')).toBeNull()
    expect(localStorage.getItem('__refresh_token__')).toBeNull()
  })
})
