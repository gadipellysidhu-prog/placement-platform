import { useMemo, useState } from 'react'
import { Award } from 'lucide-react'
import { PageHeader } from '@/shared/ui/page-header'
import { DataTable, type Column } from '@/shared/ui/data-table'
import { StatusBadge } from '@/shared/ui/status-badge'
import { formatDateTime } from '@/utils/format'
import type { CertificateResponse } from '@/lib/api'
import { useCertificates } from '../hooks/use-certificates'
import { CertificateFileLink } from '../components/CertificateFileLink'
import { CertificateReviewActions } from '../components/CertificateReviewActions'

const PAGE_SIZE = 10

/** PENDING first (so the review queue is actionable), then most recently submitted. */
const STATUS_ORDER: Record<CertificateResponse['verificationStatus'], number> = {
  PENDING: 0,
  VERIFIED: 1,
  REJECTED: 1,
}

export default function CertificateVerificationPage() {
  const [page, setPage] = useState(0)
  const { data, isLoading, isError, refetch } = useCertificates({ page, size: PAGE_SIZE })

  const rows = useMemo(() => {
    const content = data?.content ?? []
    return [...content].sort((a, b) => {
      const order = STATUS_ORDER[a.verificationStatus] - STATUS_ORDER[b.verificationStatus]
      if (order !== 0) return order
      return b.createdAt.localeCompare(a.createdAt)
    })
  }, [data])

  const columns: Column<CertificateResponse>[] = [
    {
      key: 'name',
      header: 'Certificate',
      cell: (c) => (
        <div className="min-w-0">
          <p className="truncate font-medium text-foreground">{c.name}</p>
          <p className="truncate text-xs text-muted-foreground">
            {c.issuingOrganization ?? 'Unknown issuer'}
            {c.skillName && ` · ${c.skillName}`}
          </p>
        </div>
      ),
    },
    {
      key: 'student',
      header: 'Student',
      cell: (c) => <span className="text-sm">{c.studentRollNumber}</span>,
    },
    {
      key: 'status',
      header: 'Status',
      cell: (c) => <StatusBadge status={c.verificationStatus} />,
    },
    {
      key: 'submitted',
      header: 'Submitted',
      cell: (c) => (
        <span className="text-sm text-muted-foreground">{formatDateTime(c.createdAt)}</span>
      ),
    },
    {
      key: 'document',
      header: 'Document',
      cell: (c) => <CertificateFileLink fileKey={c.fileKey} />,
    },
    {
      key: 'actions',
      header: <span className="sr-only">Actions</span>,
      className: 'text-right',
      cell: (c) => <CertificateReviewActions certificate={c} />,
    },
  ]

  return (
    <div className="space-y-6">
      <PageHeader
        title="Certificate Verification"
        description="Review student-submitted certificates. Pending items appear first."
      />

      <DataTable
        columns={columns}
        data={rows}
        isLoading={isLoading}
        isError={isError}
        onRetry={() => refetch()}
        emptyIcon={<Award />}
        emptyTitle="No certificates submitted"
        emptyDescription="Certificates submitted by students will appear here for review."
        page={page}
        totalPages={data?.totalPages ?? 1}
        onPageChange={setPage}
        getRowKey={(c) => c.id}
      />
    </div>
  )
}
