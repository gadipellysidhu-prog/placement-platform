import { QueryClient } from '@tanstack/react-query'

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 1000 * 60 * 5, // 5 min — reduce redundant refetches
      gcTime: 1000 * 60 * 10, // 10 min — keep cache warm post-unmount
      refetchOnWindowFocus: false, // avoid aggressive re-fetches on tab switch
      retry: (failureCount, error) => {
        // Never retry 4xx client errors — they won't resolve on retry
        if (
          error != null &&
          typeof error === 'object' &&
          'status' in error &&
          typeof error.status === 'number' &&
          error.status >= 400 &&
          error.status < 500
        ) {
          return false
        }
        return failureCount < 2
      },
    },
    mutations: {
      retry: false, // mutations are not idempotent by default
    },
  },
})
