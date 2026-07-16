import { apiClient } from '@/lib/axios'
import type { Page, PageParams } from '@/types'

export interface AuditLogResponse {
  id: string
  entityType: string
  entityId: string | null
  action: string
  performedBy: string | null
  correlationId: string | null
  ipAddress: string | null
  userAgent: string | null
  previousValue: string | null
  newValue: string | null
  reason: string | null
  success: boolean
  createdAt: string
}

/** Filters mirror AuditLogController exactly; dates are ISO-8601 instants. */
export interface ListAuditLogsParams extends PageParams {
  entityType?: string
  entityId?: string
  action?: string
  performedBy?: string
  dateFrom?: string
  dateTo?: string
}

/**
 * `/api/admin/audit-logs` — ADMIN only, read-only. The audit trail is written by
 * the backend; there is no create/update/delete endpoint by design.
 * Defaults to `sort=createdAt,desc` server-side.
 */
export const auditLogsApi = {
  list: (params?: ListAuditLogsParams) =>
    apiClient.get<Page<AuditLogResponse>>('/api/admin/audit-logs', { params }).then((r) => r.data),
}
