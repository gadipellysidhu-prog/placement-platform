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
import { Spinner } from '@/shared/ui/spinner'
import type { ApproveRegistrationRequest, PendingRegistrationResponse } from '@/lib/api'

interface ApproveRegistrationDialogProps {
  registration: PendingRegistrationResponse | null
  onOpenChange: (open: boolean) => void
  onConfirm: (data: ApproveRegistrationRequest) => void
  isPending?: boolean
}

const MAX_YEAR = 6
const MIN_YEAR = 1

/**
 * Collects the profile details assigned when approving a pending student
 * registration. Roll number is required and unique (the backend enforces uniqueness
 * and returns a readable 409). Branch and CGPA are set later via the student profile.
 */
export function ApproveRegistrationDialog({
  registration,
  onOpenChange,
  onConfirm,
  isPending,
}: ApproveRegistrationDialogProps) {
  const [rollNumber, setRollNumber] = useState('')
  const [currentYear, setCurrentYear] = useState(1)

  // Reset the form each time a new registration is opened.
  useEffect(() => {
    if (registration) {
      setRollNumber('')
      setCurrentYear(1)
    }
  }, [registration])

  const trimmedRoll = rollNumber.trim()
  const canSubmit = trimmedRoll.length > 0 && !isPending

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!canSubmit) return
    onConfirm({ rollNumber: trimmedRoll, currentYear })
  }

  return (
    <Dialog open={registration !== null} onOpenChange={(next) => !isPending && onOpenChange(next)}>
      <DialogContent>
        <form onSubmit={handleSubmit}>
          <DialogHeader>
            <DialogTitle>Approve registration</DialogTitle>
            <DialogDescription>
              {registration ? `Create a student profile for ${registration.email}.` : ''}
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-4 py-4">
            <div className="space-y-2">
              <Label htmlFor="approve-roll-number">Roll number</Label>
              <Input
                id="approve-roll-number"
                value={rollNumber}
                onChange={(e) => setRollNumber(e.target.value)}
                placeholder="e.g. CS2021001"
                maxLength={50}
                required
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="approve-current-year">Current year</Label>
              <Input
                id="approve-current-year"
                type="number"
                min={MIN_YEAR}
                max={MAX_YEAR}
                value={currentYear}
                onChange={(e) => {
                  const next = Number(e.target.value)
                  if (Number.isNaN(next)) return
                  setCurrentYear(Math.min(MAX_YEAR, Math.max(MIN_YEAR, next)))
                }}
              />
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
                  Approving…
                </>
              ) : (
                'Approve'
              )}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
