import { useEffect, useMemo, useState } from 'react'
import { ChevronDown, ChevronRight, RefreshCw, ScrollText } from 'lucide-react'
import { PageHeader } from '@/shared/ui/page-header'
import { Badge } from '@/shared/ui/badge'
import { Button } from '@/shared/ui/button'
import { Input } from '@/shared/ui/input'
import { Label } from '@/shared/ui/label'
import { Skeleton } from '@/shared/ui/skeleton'
import { EmptyState } from '@/shared/ui/empty-state'
import { ErrorState } from '@/shared/ui/error-state'
import { Pagination } from '@/shared/ui/pagination'
import { useDebounce } from '@/shared/hooks/use-debounce'
import { formatDateTime } from '@/utils/format'
import { cn } from '@/utils/cn'
import type { AuditLogResponse } from '@/lib/api'
import { useAuditLogs } from '../hooks/use-audit-logs'

const PAGE_SIZE = 20

/** `datetime-local` yields 'YYYY-MM-DDTHH:mm'; the API expects an ISO-8601 instant. */
function toInstant(local: string): string | undefined {
  if (!local) return undefined
  const parsed = new Date(local)
  return Number.isNaN(parsed.getTime()) ? undefined : parsed.toISOString()
}

export default function AuditLogsPage() {
  const [page, setPage] = useState(0)
  const [performedBy, setPerformedBy] = useState('')
  const [action, setAction] = useState('')
  const [dateFrom, setDateFrom] = useState('')
  const [dateTo, setDateTo] = useState('')
  const [expanded, setExpanded] = useState<string | null>(null)

  const debouncedActor = useDebounce(performedBy)
  const debouncedAction = useDebounce(action)

  const params = useMemo(
    () => ({
      page,
      size: PAGE_SIZE,
      performedBy: debouncedActor.trim() || undefined,
      action: debouncedAction.trim() || undefined,
      dateFrom: toInstant(dateFrom),
      dateTo: toInstant(dateTo),
    }),
    [page, debouncedActor, debouncedAction, dateFrom, dateTo],
  )

  useEffect(() => {
    setPage(0)
  }, [debouncedActor, debouncedAction, dateFrom, dateTo])

  const { data, isLoading, isError, refetch, isFetching } = useAuditLogs(params)

  const entries = data?.content ?? []
  const hasFilters = Boolean(
    params.performedBy || params.action || params.dateFrom || params.dateTo,
  )

  return (
    <div className="space-y-6">
      <PageHeader
        title="Audit Logs"
        description="Read-only record of every administrative action"
        action={
          <Button variant="outline" onClick={() => void refetch()} disabled={isFetching}>
            <RefreshCw className={isFetching ? 'h-4 w-4 animate-spin' : 'h-4 w-4'} />
            Refresh
          </Button>
        }
      />

      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <div className="space-y-1.5">
          <Label htmlFor="audit-actor">Actor</Label>
          <Input
            id="audit-actor"
            value={performedBy}
            onChange={(e) => setPerformedBy(e.target.value)}
            placeholder="name@university.edu"
          />
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="audit-action">Action</Label>
          <Input
            id="audit-action"
            value={action}
            onChange={(e) => setAction(e.target.value)}
            placeholder="e.g. USER_DISABLED"
          />
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="audit-from">From</Label>
          <Input
            id="audit-from"
            type="datetime-local"
            value={dateFrom}
            onChange={(e) => setDateFrom(e.target.value)}
          />
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="audit-to">To</Label>
          <Input
            id="audit-to"
            type="datetime-local"
            value={dateTo}
            onChange={(e) => setDateTo(e.target.value)}
          />
        </div>
      </div>

      {isError ? (
        <ErrorState
          title="Failed to load audit logs"
          description="An error occurred while fetching the audit trail."
          onRetry={() => void refetch()}
          className="my-8"
        />
      ) : isLoading ? (
        <div className="space-y-2">
          {Array.from({ length: 5 }).map((_, i) => (
            <Skeleton key={i} className="h-16 w-full" />
          ))}
        </div>
      ) : entries.length === 0 ? (
        <EmptyState
          icon={<ScrollText />}
          title={hasFilters ? 'No matching entries' : 'No audit entries yet'}
          description={
            hasFilters
              ? 'Try widening your filters or clearing the date range.'
              : 'Administrative actions will appear here as they happen.'
          }
        />
      ) : (
        <div className="space-y-4">
          <div className="divide-y divide-border rounded-lg border border-border">
            {entries.map((entry) => (
              <AuditLogRow
                key={entry.id}
                entry={entry}
                isExpanded={expanded === entry.id}
                onToggle={() => setExpanded(expanded === entry.id ? null : entry.id)}
              />
            ))}
          </div>
          {(data?.totalPages ?? 1) > 1 && (
            <Pagination page={page} totalPages={data?.totalPages ?? 1} onPageChange={setPage} />
          )}
        </div>
      )}
    </div>
  )
}

interface AuditLogRowProps {
  entry: AuditLogResponse
  isExpanded: boolean
  onToggle: () => void
}

function AuditLogRow({ entry, isExpanded, onToggle }: AuditLogRowProps) {
  const Chevron = isExpanded ? ChevronDown : ChevronRight

  return (
    <div>
      <button
        type="button"
        onClick={onToggle}
        aria-expanded={isExpanded}
        className="flex w-full items-center gap-3 px-4 py-3 text-left hover:bg-muted/50"
      >
        <Chevron className="h-4 w-4 shrink-0 text-muted-foreground" aria-hidden="true" />
        <Badge variant={entry.success ? 'secondary' : 'destructive'}>{entry.action}</Badge>
        <span className="min-w-0 flex-1 truncate text-sm">
          <span className="text-muted-foreground">{entry.entityType}</span>
          {entry.performedBy && <span className="ml-2">by {entry.performedBy}</span>}
        </span>
        <span className="shrink-0 text-xs text-muted-foreground">
          {formatDateTime(entry.createdAt)}
        </span>
      </button>

      {isExpanded && (
        <dl className="grid gap-x-6 gap-y-2 border-t border-border bg-muted/30 px-4 py-3 text-xs sm:grid-cols-2">
          <Field label="Entity ID" value={entry.entityId} mono />
          <Field label="Correlation ID" value={entry.correlationId} mono />
          <Field label="Previous value" value={entry.previousValue} mono />
          <Field label="New value" value={entry.newValue} mono />
          <Field label="IP address" value={entry.ipAddress} mono />
          <Field label="Reason" value={entry.reason} />
          <Field label="User agent" value={entry.userAgent} className="sm:col-span-2" />
        </dl>
      )}
    </div>
  )
}

function Field({
  label,
  value,
  mono,
  className,
}: {
  label: string
  value: string | null
  mono?: boolean
  className?: string
}) {
  return (
    <div className={className}>
      <dt className="text-muted-foreground">{label}</dt>
      <dd className={cn('break-all', mono && 'font-mono')}>
        {value ?? <span className="text-muted-foreground">—</span>}
      </dd>
    </div>
  )
}
