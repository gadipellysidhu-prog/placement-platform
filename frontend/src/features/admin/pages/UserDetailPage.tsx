import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { ArrowLeft } from 'lucide-react'
import { PageHeader } from '@/shared/ui/page-header'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/ui/card'
import { Button } from '@/shared/ui/button'
import { Badge } from '@/shared/ui/badge'
import { StatusBadge } from '@/shared/ui/status-badge'
import { Skeleton } from '@/shared/ui/skeleton'
import { ErrorState } from '@/shared/ui/error-state'
import { ConfirmDialog } from '@/shared/ui/confirm-dialog'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/shared/ui/select'
import { useToast } from '@/shared/hooks/use-toast'
import { ROUTES } from '@/constants/routes'
import { ROLES, ROLE_LABELS } from '@/constants/roles'
import { getApiErrorMessage } from '@/lib/api'
import { formatDateTime } from '@/utils/format'
import type { Role } from '@/types'
import {
  useAdminUser,
  useAssignUserRole,
  useDisableUser,
  useEnableUser,
  useLockUser,
  useUnlockUser,
} from '../hooks/use-admin-users'

/** The state transitions offered for a given account status. */
type Action = 'enable' | 'disable' | 'lock' | 'unlock'

const ACTION_COPY: Record<Action, { title: string; description: string; confirmLabel: string }> = {
  enable: {
    title: 'Enable account?',
    description: 'The user will be able to sign in again.',
    confirmLabel: 'Enable',
  },
  disable: {
    title: 'Disable account?',
    description:
      'The user will be signed out everywhere and blocked from signing in until re-enabled.',
    confirmLabel: 'Disable',
  },
  lock: {
    title: 'Lock account?',
    description: 'The user will be signed out everywhere and cannot sign in until unlocked.',
    confirmLabel: 'Lock',
  },
  unlock: {
    title: 'Unlock account?',
    description: 'The account returns to active and the user can sign in again.',
    confirmLabel: 'Unlock',
  },
}

export default function UserDetailPage() {
  const { id = '' } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const toast = useToast()

  const [pendingAction, setPendingAction] = useState<Action | null>(null)
  const [pendingRole, setPendingRole] = useState<Role | null>(null)

  const { data: user, isLoading, isError, refetch } = useAdminUser(id)

  const enable = useEnableUser()
  const disable = useDisableUser()
  const lock = useLockUser()
  const unlock = useUnlockUser()
  const assignRole = useAssignUserRole(id)

  const mutations: Record<Action, ReturnType<typeof useEnableUser>> = {
    enable,
    disable,
    lock,
    unlock,
  }

  function runAction(action: Action) {
    mutations[action].mutate(id, {
      onSuccess: () => {
        setPendingAction(null)
        toast.success(`Account ${action}d.`)
      },
      onError: (err) => {
        setPendingAction(null)
        // The backend refuses to disable/lock the last active administrator (409);
        // its message is authoritative and shown verbatim.
        toast.error(`Could not ${action} account`, getApiErrorMessage(err))
      },
    })
  }

  function runRoleChange(role: Role) {
    assignRole.mutate(
      { role },
      {
        onSuccess: () => {
          setPendingRole(null)
          toast.success(`Role changed to ${ROLE_LABELS[role]}.`)
        },
        onError: (err) => {
          setPendingRole(null)
          // 409 when demoting the last active administrator.
          toast.error('Could not change role', getApiErrorMessage(err))
        },
      },
    )
  }

  if (isLoading) {
    return (
      <div className="space-y-6">
        <Skeleton className="h-9 w-64" />
        <Skeleton className="h-64 w-full" />
      </div>
    )
  }

  if (isError || !user) {
    return (
      <ErrorState
        title="Failed to load user"
        description="This account could not be loaded."
        onRetry={() => void refetch()}
        className="my-8"
      />
    )
  }

  const isActionPending = Object.values(mutations).some((m) => m.isPending)

  return (
    <div className="space-y-6">
      <Button variant="ghost" size="sm" onClick={() => navigate(ROUTES.ADMIN.USERS)}>
        <ArrowLeft className="h-4 w-4" /> Back to users
      </Button>

      <PageHeader title={user.email} description="Account details and access control" />

      <div className="grid gap-6 md:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>Account</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4 text-sm">
            <Detail label="Status">
              <StatusBadge status={user.status} />
            </Detail>
            <Detail label="Role">{ROLE_LABELS[user.role]}</Detail>
            <Detail label="Email verified">
              <Badge variant={user.emailVerified ? 'success' : 'outline'}>
                {user.emailVerified ? 'Verified' : 'Not verified'}
              </Badge>
            </Detail>
            <Detail label="Last activity">
              {user.lastActivityAt ? (
                formatDateTime(user.lastActivityAt)
              ) : (
                <span className="text-muted-foreground">No activity recorded</span>
              )}
            </Detail>
            <Detail label="Created">{formatDateTime(user.createdAt)}</Detail>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Access</CardTitle>
          </CardHeader>
          <CardContent className="space-y-6">
            <div className="space-y-2">
              <label className="text-sm font-medium" htmlFor="user-role">
                Role
              </label>
              <Select
                value={user.role}
                onValueChange={(v) => setPendingRole(v as Role)}
                disabled={assignRole.isPending}
              >
                <SelectTrigger id="user-role" aria-label="Change role">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {Object.values(ROLES).map((r) => (
                    <SelectItem key={r} value={r}>
                      {ROLE_LABELS[r]}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-2">
              <p className="text-sm font-medium">Account state</p>
              <div className="flex flex-wrap gap-2">
                {user.status !== 'ACTIVE' && (
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={isActionPending}
                    onClick={() => setPendingAction('enable')}
                  >
                    Enable
                  </Button>
                )}
                {user.status !== 'DISABLED' && (
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={isActionPending}
                    onClick={() => setPendingAction('disable')}
                  >
                    Disable
                  </Button>
                )}
                {user.status !== 'LOCKED' && (
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={isActionPending}
                    onClick={() => setPendingAction('lock')}
                  >
                    Lock
                  </Button>
                )}
                {user.status === 'LOCKED' && (
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={isActionPending}
                    onClick={() => setPendingAction('unlock')}
                  >
                    Unlock
                  </Button>
                )}
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      <ConfirmDialog
        open={pendingAction !== null}
        onOpenChange={(next) => !next && setPendingAction(null)}
        title={pendingAction ? ACTION_COPY[pendingAction].title : ''}
        description={pendingAction ? ACTION_COPY[pendingAction].description : ''}
        confirmLabel={pendingAction ? ACTION_COPY[pendingAction].confirmLabel : ''}
        destructive={pendingAction === 'disable' || pendingAction === 'lock'}
        isPending={isActionPending}
        onConfirm={() => pendingAction && runAction(pendingAction)}
      />

      <ConfirmDialog
        open={pendingRole !== null}
        onOpenChange={(next) => !next && setPendingRole(null)}
        title="Change role?"
        description={
          pendingRole
            ? `${user.email} will be given the ${ROLE_LABELS[pendingRole]} role, changing what they can access.`
            : ''
        }
        confirmLabel="Change role"
        isPending={assignRole.isPending}
        onConfirm={() => pendingRole && runRoleChange(pendingRole)}
      />
    </div>
  )
}

function Detail({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="flex items-center justify-between gap-4">
      <span className="text-muted-foreground">{label}</span>
      <span className="text-right">{children}</span>
    </div>
  )
}
