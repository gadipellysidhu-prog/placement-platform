import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Users } from 'lucide-react'
import { PageHeader } from '@/shared/ui/page-header'
import { DataTable, type Column } from '@/shared/ui/data-table'
import { StatusBadge } from '@/shared/ui/status-badge'
import { Badge } from '@/shared/ui/badge'
import { ROUTES } from '@/constants/routes'
import { useStudents } from '../hooks/use-students'
import type { StudentResponse } from '@/lib/api'

const PAGE_SIZE = 20

const columns: Column<StudentResponse>[] = [
  {
    key: 'rollNumber',
    header: 'Roll Number',
    cell: (row) => <span className="font-medium">{row.rollNumber}</span>,
  },
  {
    key: 'email',
    header: 'Email',
    cell: (row) => <span className="text-muted-foreground">{row.userEmail}</span>,
  },
  {
    key: 'branch',
    header: 'Branch',
    cell: (row) => row.branchName ?? <span className="text-muted-foreground">—</span>,
  },
  {
    key: 'year',
    header: 'Year',
    cell: (row) => `Year ${row.currentYear}`,
    className: 'w-20',
  },
  {
    key: 'cgpa',
    header: 'CGPA',
    cell: (row) => (row.cgpa != null ? row.cgpa : <span className="text-muted-foreground">—</span>),
    className: 'w-20',
  },
  {
    key: 'status',
    header: 'Status',
    cell: (row) => <StatusBadge status={row.status} />,
    className: 'w-32',
  },
  {
    key: 'eligible',
    header: 'Eligible',
    cell: (row) => (
      <Badge variant={row.placementEligible ? 'success' : 'outline'}>
        {row.placementEligible ? 'Yes' : 'No'}
      </Badge>
    ),
    className: 'w-24',
  },
]

export default function StudentsListPage() {
  const [page, setPage] = useState(0)
  const navigate = useNavigate()

  const { data, isLoading, isError, refetch } = useStudents({ page, size: PAGE_SIZE })

  return (
    <div className="space-y-6">
      <PageHeader title="Students" description="All registered student profiles" />

      <DataTable
        columns={columns}
        data={data?.content ?? []}
        isLoading={isLoading}
        isError={isError}
        onRetry={() => refetch()}
        emptyTitle="No students yet"
        emptyDescription="Student profiles will appear here once created by placement officers."
        emptyIcon={<Users />}
        page={page}
        totalPages={data?.totalPages ?? 1}
        onPageChange={setPage}
        getRowKey={(row) => row.id}
        onRowClick={(row) => navigate(ROUTES.OFFICER.STUDENT_DETAIL(row.id))}
      />
    </div>
  )
}
