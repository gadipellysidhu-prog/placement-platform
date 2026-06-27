import { useCallback, useState } from 'react'
import { useDebounce } from './use-debounce'

interface UseSearchReturn {
  query: string
  debouncedQuery: string
  setQuery: (q: string) => void
  clear: () => void
}

/** Manages search input state with debouncing for API calls. */
export function useSearch(delay = 400): UseSearchReturn {
  const [query, setQuery] = useState('')
  const debouncedQuery = useDebounce(query, delay)
  const clear = useCallback(() => setQuery(''), [])
  return { query, debouncedQuery, setQuery, clear }
}
