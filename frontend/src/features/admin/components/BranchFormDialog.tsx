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
import { Textarea } from '@/shared/ui/textarea'
import { Spinner } from '@/shared/ui/spinner'
import type { BranchResponse, CreateBranchRequest } from '@/lib/api'

/** Mirrors the @Size constraints on BranchCreateRequest / BranchUpdateRequest. */
const MAX_NAME = 100
const MAX_CODE = 20

interface BranchFormDialogProps {
  /** The branch being edited, or null when creating. */
  branch: BranchResponse | null
  open: boolean
  onOpenChange: (open: boolean) => void
  onConfirm: (data: CreateBranchRequest) => void
  isPending?: boolean
}

export function BranchFormDialog({
  branch,
  open,
  onOpenChange,
  onConfirm,
  isPending,
}: BranchFormDialogProps) {
  const isEdit = branch !== null

  const [name, setName] = useState('')
  const [code, setCode] = useState('')
  const [description, setDescription] = useState('')

  useEffect(() => {
    if (!open) return
    setName(branch?.name ?? '')
    setCode(branch?.code ?? '')
    setDescription(branch?.description ?? '')
  }, [open, branch])

  const trimmedName = name.trim()
  const canSubmit = trimmedName.length > 0 && !isPending

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!canSubmit) return
    onConfirm({
      name: trimmedName,
      code: code.trim() || undefined,
      description: description.trim() || undefined,
    })
  }

  return (
    <Dialog open={open} onOpenChange={(next) => !isPending && onOpenChange(next)}>
      <DialogContent>
        <form onSubmit={handleSubmit}>
          <DialogHeader>
            <DialogTitle>{isEdit ? 'Edit branch' : 'New branch'}</DialogTitle>
            <DialogDescription>
              Branches drive student profiles and job posting eligibility.
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-4 py-4">
            <div className="space-y-2">
              <Label htmlFor="branch-name">Name</Label>
              <Input
                id="branch-name"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="e.g. Computer Science"
                maxLength={MAX_NAME}
                required
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="branch-code">Code</Label>
              <Input
                id="branch-code"
                value={code}
                onChange={(e) => setCode(e.target.value)}
                placeholder="e.g. CS"
                maxLength={MAX_CODE}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="branch-description">Description</Label>
              <Textarea
                id="branch-description"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                rows={2}
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
