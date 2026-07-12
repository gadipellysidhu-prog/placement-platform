import { useEffect, useRef } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { jobIntelligenceApi, queryKeys } from '@/lib/api'
import type { JobIntelligenceRun, StartRunRequest } from '@/lib/api'
import { normalizeApiError } from '@/lib/api'

/**
 * Latest AI run for a posting, polled while the pipeline is active. Poll interval
 * backs off (1s → 2s → 5s) and stops entirely once the run is terminal. A 404
 * simply means the posting has never been analyzed — treated as "no run", never
 * retried or surfaced as an error.
 */
export function useLatestJobIntelligenceRun(postingId: string, enabled = true) {
  const pollCount = useRef(0)

  const query = useQuery({
    queryKey: queryKeys.jobIntelligence.latest(postingId),
    queryFn: () => jobIntelligenceApi.latestForPosting(postingId),
    enabled: enabled && !!postingId,
    retry: (failureCount, error) => normalizeApiError(error).status !== 404 && failureCount < 2,
    refetchInterval: (q) => {
      const run = q.state.data
      if (!run || run.terminal) {
        return false
      }
      pollCount.current += 1
      if (pollCount.current < 3) return 1_000
      if (pollCount.current < 8) return 2_000
      return 5_000
    },
  })

  // Reset the backoff whenever a fresh (non-terminal) run appears.
  useEffect(() => {
    if (query.data && !query.data.terminal) {
      return
    }
    pollCount.current = 0
  }, [query.data])

  const notAnalyzed = query.isError && normalizeApiError(query.error).status === 404
  return { ...query, notAnalyzed }
}

/**
 * Invalidates posting + skills caches exactly once when a run transitions into
 * COMPLETED, so newly tagged chips and newly created catalog skills appear with
 * no manual refresh.
 */
export function useRunCompletionRefresh(postingId: string, run: JobIntelligenceRun | undefined) {
  const qc = useQueryClient()
  const lastHandled = useRef<string | null>(null)

  useEffect(() => {
    if (!run || run.status !== 'COMPLETED' || lastHandled.current === run.id) {
      return
    }
    lastHandled.current = run.id
    void qc.invalidateQueries({ queryKey: queryKeys.jobPostings.detail(postingId) })
    void qc.invalidateQueries({ queryKey: queryKeys.skills.all() })
  }, [run, postingId, qc])
}

export function useStartJobIntelligenceRun() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: StartRunRequest) => jobIntelligenceApi.startRun(data),
    onSuccess: (run) => {
      qc.setQueryData(queryKeys.jobIntelligence.latest(run.jobPostingId), run)
    },
  })
}

export function useRetryJobIntelligenceRun(postingId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (runId: string) => jobIntelligenceApi.retry(runId),
    onSuccess: (run) => {
      qc.setQueryData(queryKeys.jobIntelligence.latest(postingId), run)
    },
  })
}
