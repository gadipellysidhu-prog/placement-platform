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
import type { CreateSkillRequest, SkillResponse } from '@/lib/api'

/** Mirrors the @Size constraints on SkillCreateRequest / SkillUpdateRequest. */
const MAX_NAME = 100
const MAX_CATEGORY = 50

interface SkillFormDialogProps {
  /** The skill being edited, or null when creating. */
  skill: SkillResponse | null
  open: boolean
  onOpenChange: (open: boolean) => void
  onConfirm: (data: CreateSkillRequest) => void
  isPending?: boolean
}

/**
 * Name and category are the only fields the backend's create/update requests accept
 * — the richer catalog attributes (popularity, provenance, AI confidence) are
 * maintained by the platform, not hand-edited.
 */
export function SkillFormDialog({
  skill,
  open,
  onOpenChange,
  onConfirm,
  isPending,
}: SkillFormDialogProps) {
  const isEdit = skill !== null

  const [name, setName] = useState('')
  const [category, setCategory] = useState('')

  useEffect(() => {
    if (!open) return
    setName(skill?.name ?? '')
    setCategory(skill?.category ?? '')
  }, [open, skill])

  const trimmedName = name.trim()
  const canSubmit = trimmedName.length > 0 && !isPending

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!canSubmit) return
    onConfirm({ name: trimmedName, category: category.trim() || undefined })
  }

  return (
    <Dialog open={open} onOpenChange={(next) => !isPending && onOpenChange(next)}>
      <DialogContent>
        <form onSubmit={handleSubmit}>
          <DialogHeader>
            <DialogTitle>{isEdit ? 'Edit skill' : 'New skill'}</DialogTitle>
            <DialogDescription>
              Skills are tagged on job postings and student profiles.
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-4 py-4">
            <div className="space-y-2">
              <Label htmlFor="skill-name">Name</Label>
              <Input
                id="skill-name"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="e.g. Java"
                maxLength={MAX_NAME}
                required
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="skill-category">Category</Label>
              <Input
                id="skill-category"
                value={category}
                onChange={(e) => setCategory(e.target.value)}
                placeholder="e.g. Programming"
                maxLength={MAX_CATEGORY}
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
