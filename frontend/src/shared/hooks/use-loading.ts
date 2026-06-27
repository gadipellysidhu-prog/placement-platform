import { useCallback, useState } from 'react'

interface UseLoadingReturn {
  isLoading: boolean
  start: () => void
  stop: () => void
  wrap: <T>(fn: () => Promise<T>) => Promise<T>
}

/** Tracks loading state for imperative async operations outside of TanStack Query. */
export function useLoading(initialState = false): UseLoadingReturn {
  const [isLoading, setIsLoading] = useState(initialState)

  const start = useCallback(() => setIsLoading(true), [])
  const stop = useCallback(() => setIsLoading(false), [])

  const wrap = useCallback(async <T>(fn: () => Promise<T>): Promise<T> => {
    setIsLoading(true)
    try {
      return await fn()
    } finally {
      setIsLoading(false)
    }
  }, [])

  return { isLoading, start, stop, wrap }
}
