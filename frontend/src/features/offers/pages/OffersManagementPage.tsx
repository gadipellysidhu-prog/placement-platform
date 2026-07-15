import { useState } from 'react'
import { Gift } from 'lucide-react'
import { PageHeader } from '@/shared/ui/page-header'
import { DataTable, type Column } from '@/shared/ui/data-table'
import { StatusBadge } from '@/shared/ui/status-badge'
import { formatCTC, formatDate } from '@/utils/format'
import type { OfferResponse } from '@/lib/api'
import { useOffers } from '../hooks/use-offers'
import { ExpireOfferAction } from '../components/ExpireOfferAction'

const PAGE_SIZE = 10

export default function OffersManagementPage() {
  const [page, setPage] = useState(0)
  const { data, isLoading, isError, refetch } = useOffers({ page, size: PAGE_SIZE })

  const columns: Column<OfferResponse>[] = [
    {
      key: 'company',
      header: 'Company',
      cell: (o) => <span className="font-medium text-foreground">{o.companyName}</span>,
    },
    {
      key: 'student',
      header: 'Student',
      cell: (o) => <span className="text-sm">{o.studentRollNumber}</span>,
    },
    {
      key: 'status',
      header: 'Status',
      cell: (o) => <StatusBadge status={o.status} />,
    },
    {
      key: 'ctc',
      header: 'Package',
      cell: (o) => <span className="text-sm">{formatCTC(o.ctc)}</span>,
    },
    {
      key: 'joining',
      header: 'Joining',
      cell: (o) => (
        <span className="text-sm text-muted-foreground">{formatDate(o.joiningDate)}</span>
      ),
    },
    {
      key: 'actions',
      header: <span className="sr-only">Actions</span>,
      className: 'text-right',
      cell: (o) => <ExpireOfferAction offer={o} />,
    },
  ]

  return (
    <div className="space-y-6">
      <PageHeader
        title="Offers"
        description="Every placement offer across the platform. Pending offers can be expired."
      />

      <DataTable
        columns={columns}
        data={data?.content ?? []}
        isLoading={isLoading}
        isError={isError}
        onRetry={() => refetch()}
        emptyIcon={<Gift />}
        emptyTitle="No offers yet"
        emptyDescription="Offers created from offered applications will appear here."
        page={page}
        totalPages={data?.totalPages ?? 1}
        onPageChange={setPage}
        getRowKey={(o) => o.id}
      />
    </div>
  )
}
