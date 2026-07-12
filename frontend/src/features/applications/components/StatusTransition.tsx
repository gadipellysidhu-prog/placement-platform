import { useState } from 'react'
import { toast } from 'sonner'
import { AlertCircle } from 'lucide-react'
import { Button } from '@/shared/ui/button'
import { Alert, AlertDescription, AlertTitle } from '@/shared/ui/alert'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/shared/ui/select'
import { getApiErrorMessage } from '@/lib/api'
import type { JobApplicationResponse } from '@/lib/api'
import { titleCase } from '@/utils/format'
import { useUpdateApplicationStatus } from '../hooks/use-applications'
import { officerTransitionsFor, type OfficerSettableStatus } from '../applications.transitions'

interface StatusTransitionProps {
  application: JobApplicationResponse
}

/**
 * Officer status-transition control. Offers only the legal target statuses for the
 * application's current status (from {@link officerTransitionsFor}, mirrored from the
 * backend transition map) — never a hardcoded or illegal option. Uses the standard
 * mutation (no optimistic update); on success the detail cache is seeded from the
 * authoritative response and the tree is invalidated to refetch. Backend 409/422
 * rejections are surfaced as a readable message.
 */
export function StatusTransition({ application }: StatusTransitionProps) {
  const transitions = officerTransitionsFor(application.status)
  const [selected, setSelected] = useState<OfficerSettableStatus | ''>('')
  const [errorDetail, setErrorDetail] = useState<string | null>(null)
  const update = useUpdateApplicationStatus(application.id)

  if (transitions.length === 0) {
    return (
      <p className="text-sm text-muted-foreground">
        No further status changes are possible for a {application.status.toLowerCase()} application.
      </p>
    )
  }

  async function handleUpdate() {
    if (!selected) return
    setErrorDetail(null)
    try {
      const updated = await update.mutateAsync({ status: selected })
      toast.success(`Status updated to ${titleCase(updated.status)}`)
      setSelected('')
    } catch (err) {
      const message = getApiErrorMessage(err)
      setErrorDetail(message)
      toast.error(message)
    }
  }

  return (
    <div className="space-y-3">
      <div className="flex flex-col gap-2 sm:flex-row">
        <Select
          value={selected}
          onValueChange={(v) => setSelected(v as OfficerSettableStatus)}
          disabled={update.isPending}
        >
          <SelectTrigger className="sm:w-52" aria-label="New status">
            <SelectValue placeholder="Choose new status" />
          </SelectTrigger>
          <SelectContent>
            {transitions.map((status) => (
              <SelectItem key={status} value={status}>
                {titleCase(status)}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        <Button onClick={handleUpdate} disabled={!selected || update.isPending}>
          {update.isPending ? 'Updating…' : 'Update status'}
        </Button>
      </div>

      {errorDetail && (
        <Alert variant="destructive">
          <AlertCircle className="h-4 w-4" aria-hidden="true" />
          <AlertTitle>Couldn’t update status</AlertTitle>
          <AlertDescription>{errorDetail}</AlertDescription>
        </Alert>
      )}
    </div>
  )
}
