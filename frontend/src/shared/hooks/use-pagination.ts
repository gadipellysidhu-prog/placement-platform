import { useState } from 'react'
import type { PageParams } from '@/types'

interface UsePaginationOptions {
  initialPage?: number
  initialSize?: number
  initialSort?: string
}

interface UsePaginationReturn {
  params: Required<PageParams>
  setPage: (page: number) => void
  setSize: (size: number) => void
  setSort: (sort: string) => void
  reset: () => void
}

/** Manages page/size/sort state for paginated API calls. */
export function usePagination({
  initialPage = 0,
  initialSize = 20,
  initialSort = '',
}: UsePaginationOptions = {}): UsePaginationReturn {
  const [page, setPage] = useState(initialPage)
  const [size, setSize] = useState(initialSize)
  const [sort, setSort] = useState(initialSort)

  const params: Required<PageParams> = { page, size, sort }

  function handleSetPage(p: number) {
    setPage(p)
  }

  function handleSetSize(s: number) {
    setSize(s)
    setPage(0)
  }

  function handleSetSort(s: string) {
    setSort(s)
    setPage(0)
  }

  function reset() {
    setPage(initialPage)
    setSize(initialSize)
    setSort(initialSort)
  }

  return { params, setPage: handleSetPage, setSize: handleSetSize, setSort: handleSetSort, reset }
}
