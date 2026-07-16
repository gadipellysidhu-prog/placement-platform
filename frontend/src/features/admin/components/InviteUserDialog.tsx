import { useEffect, useState } from 'react'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/shared/ui/dialog'
import { Button } from '@/shared/ui/button'
import { Input } from '@/shared/ui/input'
import { Label } from '@/shared/ui/label'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/shared/ui/select'
import { Spinner } from '@/shared/ui/spinner'
import { ROLES, ROLE_LABELS } from '@/constants/roles'
import type { Role } from '@/types'

interface InviteUserDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  onConfirm: (data: { email: string; role: Role }) => void
  isPending?: boolean
}

/**
 * Invites a privileged user. The backend creates the account in INVITED state and
 * emails an activation link; the invitee sets their own password via
 * AcceptInvitationPage. A duplicate email is refused with a 409 the caller surfaces.
 */
export function InviteUserDialog({
  open,
  onOpenChange,
  onConfirm,
  isPending,
}: InviteUserDialogProps) {
  const [email, setEmail] = useState('')
  const [role, setRole] = useState<Role>(ROLES.PLACEMENT_OFFICER)

  // Reset each time the dialog is reopened.
  useEffect(() => {
    if (open) {
      setEmail('')
      setRole(ROLES.PLACEMENT_OFFICER)
    }
  }, [open])

  const trimmedEmail = email.trim()
  const canSubmit = trimmedEmail.length > 0 && !isPending

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!canSubmit) return
    onConfirm({ email: trimmedEmail, role })
  }

  return (
    <Dialog open={open} onOpenChange={(next) => !isPending && onOpenChange(next)}>
      <DialogContent>
        <form onSubmit={handleSubmit}>
          <DialogHeader>
            <DialogTitle>Invite user</DialogTitle>
            <DialogDescription>
              We&apos;ll email an activation link. The invitee sets their own password — the account
              stays inactive until they accept.
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-4 py-4">
            <div className="space-y-2">
              <Label htmlFor="invite-email">Email address</Label>
              <Input
                id="invite-email"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="name@university.edu"
                maxLength={255}
                required
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="invite-role">Role</Label>
              <Select value={role} onValueChange={(v) => setRole(v as Role)}>
                <SelectTrigger id="invite-role">
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
          </div>

          <DialogFooter>
            <Button
              type="button"
              variant="outline"
              onClick={() => onOpenChange(false)}
              disabled={isPending}
            >
              Cancel
            </Button>
            <Button type="submit" disabled={!canSubmit}>
              {isPending ? (
                <>
                  <Spinner size="sm" className="mr-2" />
                  Sending…
                </>
              ) : (
                'Send invitation'
              )}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
