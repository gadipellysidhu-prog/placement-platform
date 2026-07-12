import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { FileCheck2 } from 'lucide-react'
import { PageHeader } from '@/shared/ui/page-header'
import { DataTable, type Column } from '@/shared/ui/data-table'
import { StatusBadge } from '@/shared/ui/status-badge'
import { Button } from '@/shared/ui/button'
import { ROUTES } from '@/constants/routes'
import { formatDateTime } from '@/utils/format'
import type { JobApplicationResponse } from '@/lib/api'
import { useApplications } from '../hooks/use-applications'

const PAGE_SIZE = 20

const columns: Column<JobApplicationResponse>[] = [
  {
    key: 'student',
    header: 'Student',
    cell: (row) => <span className="font-medium">{row.studentRollNumber}</span>,
    className: 'w-36',
  },
  {
    key: 'posting',
    header: 'Posting',
    cell: (row) => row.jobPostingTitle,
  },
  {
    key: 'company',
    header: 'Company',
    cell: (row) => row.companyName,
  },
  {
    key: 'appliedAt',
    header: 'Applied On',
    cell: (row) => formatDateTime(row.appliedAt),
    className: 'w-44',
  },
  {
    key: 'status',
    header: 'Status',
    cell: (row) => <StatusBadge status={row.status} />,
    className: 'w-28',
  },
  {
    key: 'actions',
    header: 'Actions',
    // Row click also navigates; this is an explicit, keyboard-reachable affordance.
    cell: () => <span className="text-sm font-medium text-primary underline-offset-4">View</span>,
    className: 'w-20',
  },
]

/**
 * Officer applications pipeline. The backend list endpoint
 * (`GET /api/applications`) accepts only Spring `Pageable` params — no status or other
 * filters exist — so this page paginates the full set and does not invent filters.
 */
export default function ApplicationsPipelinePage() {
  const [page, setPage] = useState(0)
  const navigate = useNavigate()

  const { data, isLoading, isError, refetch } = useApplications({ page, size: PAGE_SIZE })

  return (
    <div className="space-y-6">
      <PageHeader title="Applications" description="Every job application across all postings." />

      <DataTable
        columns={columns}
        data={data?.content ?? []}
        isLoading={isLoading}
        isError={isError}
        onRetry={() => refetch()}
        emptyTitle="No applications yet"
        emptyDescription="Applications will appear here once students start applying."
        emptyIcon={<FileCheck2 />}
        emptyAction={
          <Button variant="outline" size="sm" onClick={() => refetch()}>
            Refresh
          </Button>
        }
        page={page}
        totalPages={data?.totalPages ?? 1}
        onPageChange={setPage}
        getRowKey={(row) => row.id}
        onRowClick={(row) => navigate(ROUTES.OFFICER.APPLICATION_DETAIL(row.id))}
      />
    </div>
  )
}
