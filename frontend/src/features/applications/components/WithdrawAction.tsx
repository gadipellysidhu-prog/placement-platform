import { useState } from 'react'
import { toast } from 'sonner'
import { Button } from '@/shared/ui/button'
import { ConfirmDialog } from '@/shared/ui/confirm-dialog'
import { getApiErrorMessage } from '@/lib/api'
import type { JobApplicationResponse } from '@/lib/api'
import { useWithdrawApplication } from '../hooks/use-applications'
import { canWithdraw } from '../applications.transitions'

interface WithdrawActionProps {
  application: JobApplicationResponse
  /** Full-width button (used in the detail sidebar). */
  fullWidth?: boolean
}

/**
 * Reusable student withdraw control: a confirm dialog wired to the withdraw endpoint.
 * Renders nothing for terminal states (mirrors the backend, which rejects those with
 * 422), so callers don't need to guard visibility themselves. Backend 409/422 rejects
 * are surfaced as a readable toast.
 */
export function WithdrawAction({ application, fullWidth }: WithdrawActionProps) {
  const [open, setOpen] = useState(false)
  const withdraw = useWithdrawApplication(application.id)

  if (!canWithdraw(application.status)) {
    return null
  }

  async function handleConfirm() {
    try {
      await withdraw.mutateAsync()
      toast.success('Application withdrawn')
      setOpen(false)
    } catch (err) {
      // Readable handling of a backend 409/422 (e.g. a race where the status advanced).
      toast.error(getApiErrorMessage(err))
    }
  }

  return (
    <>
      <Button
        variant="outline"
        size="sm"
        className={fullWidth ? 'w-full' : undefined}
        onClick={() => setOpen(true)}
        disabled={withdraw.isPending}
      >
        Withdraw
      </Button>
      <ConfirmDialog
        open={open}
        onOpenChange={setOpen}
        title="Withdraw this application?"
        description="This withdraws your application for this posting. You won’t be able to apply to it again."
        confirmLabel="Withdraw application"
        destructive
        isPending={withdraw.isPending}
        onConfirm={handleConfirm}
      />
    </>
  )
}
