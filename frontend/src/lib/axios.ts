import axios, { type AxiosRequestConfig } from 'axios'
import { env } from '@/config/env'

export const apiClient = axios.create({
  baseURL: env.VITE_API_BASE_URL,
  headers: { 'Content-Type': 'application/json' },
  timeout: 30_000,
})

// Tracks whether a token refresh is in progress to prevent concurrent refresh storms
let isRefreshing = false
// Queue of resolve/reject callbacks waiting for the refresh to complete
let failedQueue: Array<{
  resolve: (token: string) => void
  reject: (err: unknown) => void
}> = []

function processQueue(error: unknown, token: string | null): void {
  failedQueue.forEach((item) => {
    if (error) {
      item.reject(error)
    } else {
      item.resolve(token as string)
    }
  })
  failedQueue = []
}

// REQUEST INTERCEPTOR — attach JWT from sessionStorage
apiClient.interceptors.request.use(
  (config) => {
    const token = sessionStorage.getItem('__access_token__')
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  (error: unknown) => Promise.reject(error),
)

// RESPONSE INTERCEPTOR — global error handling + silent JWT refresh on 401
apiClient.interceptors.response.use(
  (response) => response,
  async (error: unknown) => {
    if (!axios.isAxiosError(error)) return Promise.reject(error)

    const originalRequest = error.config as AxiosRequestConfig & { _retry?: boolean }

    if (error.response?.status === 401 && !originalRequest._retry) {
      const refreshToken = localStorage.getItem('__refresh_token__')

      // No refresh token — clear auth and reject immediately
      if (!refreshToken) {
        _handleAuthFailure()
        return Promise.reject(error)
      }

      if (isRefreshing) {
        // Another refresh is in flight — queue this request
        return new Promise<string>((resolve, reject) => {
          failedQueue.push({ resolve, reject })
        }).then((token) => {
          if (originalRequest.headers) {
            originalRequest.headers['Authorization'] = `Bearer ${token}`
          }
          return apiClient(originalRequest)
        })
      }

      originalRequest._retry = true
      isRefreshing = true

      try {
        const response = await axios.post<{
          accessToken: string
          refreshToken: string
          accessTokenExpiresIn: number
          tokenType: 'Bearer'
        }>(
          `${env.VITE_API_BASE_URL}/auth/refresh`,
          { refreshToken },
          { headers: { 'Content-Type': 'application/json' } },
        )

        const { accessToken, refreshToken: newRefreshToken } = response.data

        sessionStorage.setItem('__access_token__', accessToken)
        localStorage.setItem('__refresh_token__', newRefreshToken)

        // Lazily import the auth store to avoid circular dependency at module init
        const { useAuthStore } = await import('@/stores/auth.store')
        useAuthStore.getState().setAccessToken(accessToken)

        processQueue(null, accessToken)

        if (originalRequest.headers) {
          originalRequest.headers['Authorization'] = `Bearer ${accessToken}`
        }
        return apiClient(originalRequest)
      } catch (refreshError) {
        processQueue(refreshError, null)
        _handleAuthFailure()
        return Promise.reject(refreshError)
      } finally {
        isRefreshing = false
      }
    }

    return Promise.reject(error)
  },
)

function _handleAuthFailure(): void {
  // clearAuth removes both tokens from storage and resets Zustand state
  import('@/stores/auth.store').then(({ useAuthStore }) => {
    useAuthStore.getState().clearAuth()
  })
  window.location.href = '/login'
}
