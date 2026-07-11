import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Briefcase } from 'lucide-react'
import { PageHeader } from '@/shared/ui/page-header'
import { DataTable, type Column } from '@/shared/ui/data-table'
import { StatusBadge } from '@/shared/ui/status-badge'
import { SearchInput } from '@/shared/ui/search-input'
import { useDebounce } from '@/shared/hooks/use-debounce'
import { ROUTES } from '@/constants/routes'
import { formatCTCRange, formatDate } from '@/utils/format'
import type { JobPostingResponse } from '@/lib/api'
import { useJobPostings } from '../hooks/use-job-postings'

const PAGE_SIZE = 20

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

export default function JobPostingsListPage() {
  const [page, setPage] = useState(0)
  const [search, setSearch] = useState('')
  const navigate = useNavigate()

  const debouncedSearch = useDebounce(search)
  const title = debouncedSearch.trim() || undefined

  // Reset to the first page whenever the active filter changes.
  useEffect(() => {
    setPage(0)
  }, [title])

  const { data, isLoading, isError, refetch } = useJobPostings({ page, size: PAGE_SIZE, title })

  return (
    <div className="space-y-6">
      <PageHeader
        title="Browse Jobs"
        description="Open positions accepting applications right now"
      />

      <SearchInput
        placeholder="Search roles by title…"
        value={search}
        onChange={(e) => setSearch(e.target.value)}
        onClear={() => setSearch('')}
        className="max-w-sm"
        aria-label="Search job postings by title"
      />

      <DataTable
        columns={columns}
        data={data?.content ?? []}
        isLoading={isLoading}
        isError={isError}
        onRetry={() => refetch()}
        emptyTitle={title ? 'No matching jobs' : 'No open jobs right now'}
        emptyDescription={
          title
            ? 'Try a different search term.'
            : 'Check back later — new openings appear here once published.'
        }
        emptyIcon={<Briefcase />}
        page={page}
        totalPages={data?.totalPages ?? 1}
        onPageChange={setPage}
        getRowKey={(row) => row.id}
        onRowClick={(row) => navigate(ROUTES.STUDENT.JOB_POSTING_DETAIL(row.id))}
      />
    </div>
  )
}
