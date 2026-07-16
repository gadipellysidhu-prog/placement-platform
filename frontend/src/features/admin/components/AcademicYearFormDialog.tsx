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
import type { AcademicYearResponse, CreateAcademicYearRequest } from '@/lib/api'

/** Mirrors @Size(max = 20) on AcademicYearCreateRequest.label. */
const MAX_LABEL = 20

interface AcademicYearFormDialogProps {
  /** The year being edited, or null when creating. */
  year: AcademicYearResponse | null
  open: boolean
  onOpenChange: (open: boolean) => void
  onConfirm: (data: CreateAcademicYearRequest) => void
  isPending?: boolean
}

export function AcademicYearFormDialog({
  year,
  open,
  onOpenChange,
  onConfirm,
  isPending,
}: AcademicYearFormDialogProps) {
  const isEdit = year !== null

  const [label, setLabel] = useState('')
  const [startDate, setStartDate] = useState('')
  const [endDate, setEndDate] = useState('')

  useEffect(() => {
    if (!open) return
    setLabel(year?.label ?? '')
    setStartDate(year?.startDate ?? '')
    setEndDate(year?.endDate ?? '')
  }, [open, year])

  const trimmedLabel = label.trim()
  const canSubmit = trimmedLabel.length > 0 && !!startDate && !!endDate && !isPending

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!canSubmit) return
    onConfirm({ label: trimmedLabel, startDate, endDate })
  }

  return (
    <Dialog open={open} onOpenChange={(next) => !isPending && onOpenChange(next)}>
      <DialogContent>
        <form onSubmit={handleSubmit}>
          <DialogHeader>
            <DialogTitle>{isEdit ? 'Edit academic year' : 'New academic year'}</DialogTitle>
            <DialogDescription>
              {isEdit
                ? 'Only the date range can be changed.'
                : 'Creating a year does not activate it — activate it separately.'}
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-4 py-4">
            <div className="space-y-2">
              <Label htmlFor="year-label">Label</Label>
              <Input
                id="year-label"
                value={label}
                onChange={(e) => setLabel(e.target.value)}
                placeholder="e.g. 2026-27"
                maxLength={MAX_LABEL}
                // The backend's update request carries dates only; the label is fixed.
                disabled={isEdit}
                required
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="year-start">Start date</Label>
              <Input
                id="year-start"
                type="date"
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
                required
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="year-end">End date</Label>
              <Input
                id="year-end"
                type="date"
                value={endDate}
                onChange={(e) => setEndDate(e.target.value)}
                required
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
                  Saving…
                </>
              ) : (
                'Save'
              )}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
