import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { RefreshCw, UserPlus, Users } from 'lucide-react'
import { PageHeader } from '@/shared/ui/page-header'
import { DataTable, type Column } from '@/shared/ui/data-table'
import { StatusBadge } from '@/shared/ui/status-badge'
import { Badge } from '@/shared/ui/badge'
import { Button } from '@/shared/ui/button'
import { SearchInput } from '@/shared/ui/search-input'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/shared/ui/select'
import { useDebounce } from '@/shared/hooks/use-debounce'
import { useToast } from '@/shared/hooks/use-toast'
import { ROUTES } from '@/constants/routes'
import { ROLES, ROLE_LABELS } from '@/constants/roles'
import { getApiErrorMessage } from '@/lib/api'
import { formatDateTime } from '@/utils/format'
import type { AccountStatus, AdminUserResponse } from '@/lib/api'
import type { Role } from '@/types'
import { useAdminUsers, useInviteUser } from '../hooks/use-admin-users'
import { InviteUserDialog } from '../components/InviteUserDialog'

const PAGE_SIZE = 20
const ALL = 'ALL'

const STATUS_FILTERS: { value: AccountStatus | typeof ALL; label: string }[] = [
  { value: ALL, label: 'All statuses' },
  { value: 'ACTIVE', label: 'Active' },
  { value: 'INVITED', label: 'Invited' },
  { value: 'DISABLED', label: 'Disabled' },
  { value: 'LOCKED', label: 'Locked' },
]

const ROLE_FILTERS: { value: Role | typeof ALL; label: string }[] = [
  { value: ALL, label: 'All roles' },
  ...Object.values(ROLES).map((r) => ({ value: r, label: ROLE_LABELS[r] })),
]

const columns: Column<AdminUserResponse>[] = [
  {
    key: 'email',
    header: 'Email',
    cell: (row) => (
      <div className="flex items-center gap-2">
        <span className="font-medium">{row.email}</span>
        {!row.emailVerified && (
          <Badge variant="outline" className="text-xs">
            Unverified
          </Badge>
        )}
      </div>
    ),
  },
  {
    key: 'role',
    header: 'Role',
    cell: (row) => ROLE_LABELS[row.role],
    className: 'w-44',
  },
  {
    key: 'status',
    header: 'Status',
    cell: (row) => <StatusBadge status={row.status} />,
    className: 'w-28',
  },
  {
    key: 'lastActivity',
    header: 'Last activity',
    // Null means the backend holds no activity record — never a fabricated date.
    cell: (row) =>
      row.lastActivityAt ? (
        formatDateTime(row.lastActivityAt)
      ) : (
        <span className="text-muted-foreground">No activity recorded</span>
      ),
    className: 'w-52',
  },
]

export default function UsersListPage() {
  const [page, setPage] = useState(0)
  const [search, setSearch] = useState('')
  const [roleFilter, setRoleFilter] = useState<Role | typeof ALL>(ALL)
  const [statusFilter, setStatusFilter] = useState<AccountStatus | typeof ALL>(ALL)
  const [inviteOpen, setInviteOpen] = useState(false)

  const navigate = useNavigate()
  const toast = useToast()
  const debouncedSearch = useDebounce(search)

  const params = useMemo(
    () => ({
      page,
      size: PAGE_SIZE,
      query: debouncedSearch.trim() || undefined,
      role: roleFilter === ALL ? undefined : roleFilter,
      status: statusFilter === ALL ? undefined : statusFilter,
    }),
    [page, debouncedSearch, roleFilter, statusFilter],
  )

  // Any filter change invalidates the current page offset.
  useEffect(() => {
    setPage(0)
  }, [debouncedSearch, roleFilter, statusFilter])

  const { data, isLoading, isError, refetch, isFetching } = useAdminUsers(params)
  const invite = useInviteUser()

  function handleInvite(payload: { email: string; role: Role }) {
    invite.mutate(payload, {
      onSuccess: (res) => {
        setInviteOpen(false)
        toast.success(res.message)
      },
      // Surfaces the backend's own message — e.g. 409 "Email is already registered."
      onError: (err) => toast.error('Could not send invitation', getApiErrorMessage(err)),
    })
  }

  const hasFilters = Boolean(params.query || params.role || params.status)

  return (
    <div className="space-y-6">
      <PageHeader
        title="Users"
        description="Manage accounts, roles, and access across the platform"
        action={
          <div className="flex items-center gap-2">
            <Button variant="outline" onClick={() => void refetch()} disabled={isFetching}>
              <RefreshCw className={isFetching ? 'h-4 w-4 animate-spin' : 'h-4 w-4'} />
              Refresh
            </Button>
            <Button onClick={() => setInviteOpen(true)}>
              <UserPlus className="h-4 w-4" /> Invite user
            </Button>
          </div>
        }
      />

      <div className="flex flex-wrap items-center gap-3">
        <SearchInput
          className="w-full sm:w-72"
          placeholder="Search by email…"
          aria-label="Search users by email"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          onClear={() => setSearch('')}
        />
        <Select value={roleFilter} onValueChange={(v) => setRoleFilter(v as Role | typeof ALL)}>
          <SelectTrigger className="w-48" aria-label="Filter by role">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            {ROLE_FILTERS.map((opt) => (
              <SelectItem key={opt.value} value={opt.value}>
                {opt.label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        <Select
          value={statusFilter}
          onValueChange={(v) => setStatusFilter(v as AccountStatus | typeof ALL)}
        >
          <SelectTrigger className="w-44" aria-label="Filter by status">
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
        onRetry={() => void refetch()}
        emptyTitle={hasFilters ? 'No matching users' : 'No users yet'}
        emptyDescription={
          hasFilters ? 'Try a different search or filter.' : 'Invite a colleague to get started.'
        }
        emptyIcon={<Users />}
        emptyAction={
          !hasFilters ? (
            <Button variant="outline" size="sm" onClick={() => setInviteOpen(true)}>
              <UserPlus className="h-4 w-4" /> Invite user
            </Button>
          ) : undefined
        }
        page={page}
        totalPages={data?.totalPages ?? 1}
        onPageChange={setPage}
        getRowKey={(row) => row.id}
        onRowClick={(row) => navigate(ROUTES.ADMIN.USER_DETAIL(row.id))}
      />

      <InviteUserDialog
        open={inviteOpen}
        onOpenChange={setInviteOpen}
        onConfirm={handleInvite}
        isPending={invite.isPending}
      />
    </div>
  )
}
