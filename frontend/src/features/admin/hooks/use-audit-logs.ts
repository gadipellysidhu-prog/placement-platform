import { useQuery } from '@tanstack/react-query'
import { auditLogsApi, queryKeys } from '@/lib/api'
import type { ListAuditLogsParams } from '@/lib/api'

/** Read-only: the audit trail is written by the backend, never by this UI. */
export function useAuditLogs(params?: ListAuditLogsParams) {
  return useQuery({
    queryKey: queryKeys.auditLogs.list(params as Record<string, unknown> | undefined),
    queryFn: () => auditLogsApi.list(params),
    staleTime: 30_000,
  })
}
