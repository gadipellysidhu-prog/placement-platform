import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Briefcase, Plus } from 'lucide-react'
import { PageHeader } from '@/shared/ui/page-header'
import { DataTable, type Column } from '@/shared/ui/data-table'
import { StatusBadge } from '@/shared/ui/status-badge'
import { Button } from '@/shared/ui/button'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/shared/ui/select'
import { ROUTES } from '@/constants/routes'
import { formatCTCRange, formatDate } from '@/utils/format'
import type { JobPostingResponse, JobPostingStatus } from '@/lib/api'
import { useManagedJobPostings } from '../hooks/use-job-postings'

const PAGE_SIZE = 20

const STATUS_FILTERS: { value: JobPostingStatus | 'ALL'; label: string }[] = [
  { value: 'ALL', label: 'All statuses' },
  { value: 'DRAFT', label: 'Draft' },
  { value: 'OPEN', label: 'Open' },
  { value: 'CLOSED', label: 'Closed' },
  { value: 'CANCELLED', label: 'Cancelled' },
]

const columns: Column<JobPostingResponse>[] = [
  {
    key: 'title',
    header: 'Role',
    cell: (row) => <span className="font-medium">{row.title}</span>,
  },
  {
    key: 'company',
    header: 'Company',
    cell: (row) => row.companyName,
  },
  {
    key: 'ctc',
    header: 'CTC',
    cell: (row) => formatCTCRange(row.ctcMin, row.ctcMax),
    className: 'w-44',
  },
  {
    key: 'deadline',
    header: 'Deadline',
    cell: (row) =>
      row.applicationDeadline ? (
        formatDate(row.applicationDeadline)
      ) : (
        <span className="text-muted-foreground">—</span>
      ),
    className: 'w-36',
  },
  {
    key: 'status',
    header: 'Status',
    cell: (row) => <StatusBadge status={row.status} />,
    className: 'w-28',
  },
]

export default function ManageJobPostingsPage() {
  const [page, setPage] = useState(0)
  const [statusFilter, setStatusFilter] = useState<JobPostingStatus | 'ALL'>('ALL')
  const navigate = useNavigate()

  const status = statusFilter === 'ALL' ? undefined : statusFilter

  // Reset to the first page whenever the status filter changes.
  useEffect(() => {
    setPage(0)
  }, [status])

  const { data, isLoading, isError, refetch } = useManagedJobPostings({
    page,
    size: PAGE_SIZE,
    status,
  })

  return (
    <div className="space-y-6">
      <PageHeader
        title="Manage Job Postings"
        description="Create, edit, and manage the lifecycle of every posting"
        action={
          <Button asChild>
            <Link to={ROUTES.OFFICER.CREATE_JOB_POSTING}>
              <Plus className="h-4 w-4" /> Create posting
            </Link>
          </Button>
        }
      />

      <div className="flex items-center gap-3">
        <Select
          value={statusFilter}
          onValueChange={(v) => setStatusFilter(v as JobPostingStatus | 'ALL')}
        >
          <SelectTrigger className="w-48" aria-label="Filter by status">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            {STATUS_FILTERS.map((opt) => (
              <SelectItem key={opt.value} value={opt.value}>
                {opt.label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      <DataTable
        columns={columns}
        data={data?.content ?? []}
        isLoading={isLoading}
        isError={isError}
        onRetry={() => refetch()}
        emptyTitle={status ? `No ${status.toLowerCase()} postings` : 'No job postings yet'}
        emptyDescription={
          status
            ? 'Try a different status filter.'
            : 'Create your first posting to start accepting applications.'
        }
        emptyIcon={<Briefcase />}
        emptyAction={
          !status ? (
            <Button asChild variant="outline" size="sm">
              <Link to={ROUTES.OFFICER.CREATE_JOB_POSTING}>
                <Plus className="h-4 w-4" /> Create posting
              </Link>
            </Button>
          ) : undefined
        }
        page={page}
        totalPages={data?.totalPages ?? 1}
        onPageChange={setPage}
        getRowKey={(row) => row.id}
        onRowClick={(row) => navigate(ROUTES.OFFICER.JOB_POSTING_DETAIL(row.id))}
      />
    </div>
  )
}
