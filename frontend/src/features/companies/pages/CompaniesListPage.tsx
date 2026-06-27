import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { Building2, Plus } from 'lucide-react'
import { PageHeader } from '@/shared/ui/page-header'
import { DataTable, type Column } from '@/shared/ui/data-table'
import { StatusBadge } from '@/shared/ui/status-badge'
import { Button } from '@/shared/ui/button'
import { ROUTES } from '@/constants/routes'
import { ROLES } from '@/constants/roles'
import { useAuthStore } from '@/stores/auth.store'
import { useCompanies } from '../hooks/use-companies'
import type { CompanyResponse } from '@/lib/api'

const PAGE_SIZE = 20

const columns: Column<CompanyResponse>[] = [
  {
    key: 'name',
    header: 'Company',
    cell: (row) => <span className="font-medium">{row.name}</span>,
  },
  {
    key: 'industry',
    header: 'Industry',
    cell: (row) => row.industry ?? <span className="text-muted-foreground">—</span>,
  },
  {
    key: 'website',
    header: 'Website',
    cell: (row) =>
      row.website ? (
        <a
          href={row.website}
          target="_blank"
          rel="noopener noreferrer"
          className="text-primary underline-offset-4 hover:underline"
          onClick={(e) => e.stopPropagation()}
        >
          {row.website.replace(/^https?:\/\//, '')}
        </a>
      ) : (
        <span className="text-muted-foreground">—</span>
      ),
  },
  {
    key: 'status',
    header: 'Status',
    cell: (row) => <StatusBadge status={row.status} />,
    className: 'w-32',
  },
]

export default function CompaniesListPage() {
  const [page, setPage] = useState(0)
  const navigate = useNavigate()
  const user = useAuthStore((s) => s.user)

  const isOfficer = user?.role === ROLES.PLACEMENT_OFFICER || user?.role === ROLES.ADMIN

  const { data, isLoading, isError, refetch } = useCompanies({ page, size: PAGE_SIZE })

  return (
    <div className="space-y-6">
      <PageHeader
        title="Companies"
        description="Companies registered in the placement platform"
        action={
          isOfficer ? (
            <Button asChild>
              <Link to={ROUTES.OFFICER.CREATE_COMPANY}>
                <Plus className="h-4 w-4" /> Add company
              </Link>
            </Button>
          ) : undefined
        }
      />

      <DataTable
        columns={columns}
        data={data?.content ?? []}
        isLoading={isLoading}
        isError={isError}
        onRetry={() => refetch()}
        emptyTitle="No companies yet"
        emptyDescription="Companies will appear here once added by placement officers."
        emptyIcon={<Building2 />}
        emptyAction={
          isOfficer ? (
            <Button asChild variant="outline" size="sm">
              <Link to={ROUTES.OFFICER.CREATE_COMPANY}>
                <Plus className="h-4 w-4" /> Add first company
              </Link>
            </Button>
          ) : undefined
        }
        page={page}
        totalPages={data?.totalPages ?? 1}
        onPageChange={setPage}
        getRowKey={(row) => row.id}
        onRowClick={(row) => navigate(ROUTES.OFFICER.COMPANY_DETAIL(row.id))}
      />
    </div>
  )
}
