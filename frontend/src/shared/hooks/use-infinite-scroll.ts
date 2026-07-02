import { useCallback, useRef } from 'react'

interface UseInfiniteScrollOptions {
  /** Fires when the sentinel element enters the viewport. */
  onLoadMore: () => void
  /** Set to false to prevent further loads (e.g. all pages fetched). */
  hasMore: boolean
  /** Whether a fetch is currently in progress — prevents double-firing. */
  isFetching: boolean
  threshold?: number
}

/**
 * Returns a ref to attach to a sentinel element at the bottom of a list.
 * When the sentinel enters the viewport and `hasMore` is true, `onLoadMore` fires.
 */
export function useInfiniteScroll({
  onLoadMore,
  hasMore,
  isFetching,
  threshold = 0.1,
}: UseInfiniteScrollOptions) {
  const observerRef = useRef<IntersectionObserver | null>(null)

  const sentinelRef = useCallback(
    (node: HTMLElement | null) => {
      if (observerRef.current) observerRef.current.disconnect()
      if (!node || !hasMore || isFetching) return

      observerRef.current = new IntersectionObserver(
        (entries) => {
          if (entries[0].isIntersecting && hasMore && !isFetching) {
            onLoadMore()
          }
        },
        { threshold },
      )
      observerRef.current.observe(node)
    },
    [onLoadMore, hasMore, isFetching, threshold],
  )

  return { sentinelRef }
}
