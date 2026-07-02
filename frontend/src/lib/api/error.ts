import axios from 'axios'
import type { ProblemDetail, ApiError } from '@/types/api'

/** Normalize any thrown value into a typed ApiError. */
export function normalizeApiError(err: unknown): ApiError {
  if (axios.isAxiosError(err)) {
    const status = err.response?.status ?? 0
    const data = err.response?.data as Partial<ProblemDetail> | undefined

    const problem: ProblemDetail = {
      title: data?.title ?? httpStatusTitle(status),
      status,
      detail: data?.detail,
      type: data?.type,
      errors: data?.errors,
    }

    return { status, problem, message: problem.detail ?? problem.title }
  }

  if (err instanceof Error) {
    return {
      status: 0,
      problem: { title: 'Unexpected Error', status: 0, detail: err.message },
      message: err.message,
    }
  }

  return {
    status: 0,
    problem: { title: 'Unknown Error', status: 0 },
    message: 'An unknown error occurred',
  }
}

function httpStatusTitle(status: number): string {
  const titles: Record<number, string> = {
    400: 'Bad Request',
    401: 'Unauthorized',
    403: 'Forbidden',
    404: 'Not Found',
    409: 'Conflict',
    422: 'Unprocessable Entity',
    429: 'Too Many Requests',
    500: 'Internal Server Error',
  }
  return titles[status] ?? `HTTP Error ${status}`
}

/** Returns a user-friendly message for common API error statuses. */
export function getApiErrorMessage(err: unknown): string {
  const apiError = normalizeApiError(err)
  switch (apiError.status) {
    case 401:
      return 'Your session has expired. Please log in again.'
    case 403:
      return 'You do not have permission to perform this action.'
    case 404:
      return 'The requested resource was not found.'
    case 409:
      return apiError.message || 'A conflict occurred. The resource may already exist.'
    case 422:
      return apiError.message || 'The request could not be processed.'
    case 429:
      return 'Too many requests. Please wait a moment and try again.'
    case 0:
      return 'Network error. Please check your connection.'
    default:
      return apiError.message || 'An unexpected error occurred. Please try again.'
  }
}
