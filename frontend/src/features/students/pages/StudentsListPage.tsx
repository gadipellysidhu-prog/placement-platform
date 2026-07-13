import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Users, UserPlus, Clock } from 'lucide-react'
import { toast } from 'sonner'
import { PageHeader } from '@/shared/ui/page-header'
import { DataTable, type Column } from '@/shared/ui/data-table'
import { StatusBadge } from '@/shared/ui/status-badge'
import { Badge } from '@/shared/ui/badge'
import { Button } from '@/shared/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/ui/card'
import { ROUTES } from '@/constants/routes'
import { getApiErrorMessage } from '@/lib/api'
import { formatDateTime } from '@/utils/format'
import type {
  ApproveRegistrationRequest,
  PendingRegistrationResponse,
  StudentResponse,
} from '@/lib/api'
import { useApproveRegistration, usePendingRegistrations, useStudents } from '../hooks/use-students'
import { ApproveRegistrationDialog } from '../components/ApproveRegistrationDialog'

const PAGE_SIZE = 20
const PENDING_PAGE_SIZE = 50

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
  const pending = usePendingRegistrations({ size: PENDING_PAGE_SIZE, sort: 'createdAt,asc' })
  const approve = useApproveRegistration()

  const [selected, setSelected] = useState<PendingRegistrationResponse | null>(null)

  function handleApprove(reg: ApproveRegistrationRequest) {
    if (!selected) return
    const registration = selected
    approve.mutate(
      { userId: registration.userId, data: reg },
      {
        onSuccess: () => {
          toast.success(`Approved ${registration.email}`)
          setSelected(null)
        },
        onError: (err) => toast.error(getApiErrorMessage(err)),
      },
    )
  }

  const pendingItems = pending.data?.content ?? []
  const hasPending = pendingItems.length > 0

  return (
    <div className="space-y-6">
      <PageHeader title="Students" description="All registered student profiles" />

      {/* Pending registrations — only shown when there is something to review. */}
      {(hasPending || pending.isError) && (
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-base">
              <Clock className="h-4 w-4 text-warning" aria-hidden="true" />
              Pending Registrations
              {hasPending && (
                <Badge variant="warning" className="ml-1">
                  {pendingItems.length}
                </Badge>
              )}
            </CardTitle>
          </CardHeader>
          <CardContent>
            {pending.isError ? (
              <div className="flex items-center justify-between gap-4">
                <p className="text-sm text-muted-foreground">
                  Couldn’t load pending registrations.
                </p>
                <Button variant="outline" size="sm" onClick={() => pending.refetch()}>
                  Retry
                </Button>
              </div>
            ) : (
              <ul className="divide-y divide-border">
                {pendingItems.map((reg) => (
                  <li
                    key={reg.userId}
                    className="flex flex-wrap items-center justify-between gap-3 py-3 first:pt-0 last:pb-0"
                  >
                    <div className="min-w-0">
                      <p className="truncate text-sm font-medium text-foreground">
                        {reg.displayName || reg.email}
                      </p>
                      <p className="truncate text-xs text-muted-foreground">
                        {reg.email} · Registered {formatDateTime(reg.createdAt)}
                        {!reg.emailVerified && (
                          <span className="ml-1 text-warning">· awaiting email verification</span>
                        )}
                      </p>
                    </div>
                    <Button
                      size="sm"
                      onClick={() => setSelected(reg)}
                      disabled={approve.isPending || !reg.emailVerified}
                      title={
                        reg.emailVerified
                          ? undefined
                          : 'The student must verify their email before they can be approved.'
                      }
                    >
                      <UserPlus className="h-4 w-4" /> Approve
                    </Button>
                  </li>
                ))}
              </ul>
            )}
          </CardContent>
        </Card>
      )}

      <DataTable
        columns={columns}
        data={data?.content ?? []}
        isLoading={isLoading}
        isError={isError}
        onRetry={() => refetch()}
        emptyTitle="No students yet"
        emptyDescription="Student profiles will appear here once officers approve registrations."
        emptyIcon={<Users />}
        page={page}
        totalPages={data?.totalPages ?? 1}
        onPageChange={setPage}
        getRowKey={(row) => row.id}
        onRowClick={(row) => navigate(ROUTES.OFFICER.STUDENT_DETAIL(row.id))}
      />

      <ApproveRegistrationDialog
        registration={selected}
        onOpenChange={(next) => !next && setSelected(null)}
        onConfirm={handleApprove}
        isPending={approve.isPending}
      />
    </div>
  )
}
