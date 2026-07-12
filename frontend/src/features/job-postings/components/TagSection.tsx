import { useState } from 'react'
import { Plus, X } from 'lucide-react'
import { Badge } from '@/shared/ui/badge'
import { Button } from '@/shared/ui/button'
import { Spinner } from '@/shared/ui/spinner'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/shared/ui/select'

export interface TagOption {
  id: string
  name: string
}

interface TagSectionProps {
  /** Human label for a single item, e.g. "skill" or "branch" — used in placeholder/empty copy. */
  noun: string
  /** Plural label, e.g. "skills" or "branches". Defaults to `${noun}s`. */
  nounPlural?: string
  /** Currently attached items. */
  items: TagOption[]
  /** All selectable options (already-attached ones are filtered out). */
  options: TagOption[]
  optionsLoading?: boolean
  addPending?: boolean
  removePendingId?: string | null
  onAdd: (id: string) => void
  onRemove: (id: string) => void
  /** Optional replacement for the built-in Select+Add picker (e.g. search combobox). */
  picker?: React.ReactNode
}

/**
 * Officer-facing editor for a set of tagged references (required skills or eligible
 * branches). Renders the current items as removable chips plus a picker that only lists
 * options not yet attached. Purely presentational — all persistence is delegated to the
 * `onAdd`/`onRemove` callbacks supplied by the parent's mutation hooks.
 */
export function TagSection({
  noun,
  nounPlural,
  items,
  options,
  optionsLoading,
  addPending,
  removePendingId,
  onAdd,
  onRemove,
  picker,
}: TagSectionProps) {
  const [selected, setSelected] = useState<string>('')
  const plural = nounPlural ?? `${noun}s`

  const attachedIds = new Set(items.map((i) => i.id))
  const available = options.filter((o) => !attachedIds.has(o.id))

  function handleAdd() {
    if (!selected) return
    onAdd(selected)
    setSelected('')
  }

  return (
    <div className="space-y-3">
      {items.length > 0 ? (
        <ul className="flex flex-wrap gap-2">
          {items.map((item) => {
            const removing = removePendingId === item.id
            return (
              <li key={item.id}>
                <Badge variant="secondary" className="gap-1 pr-1">
                  {item.name}
                  <button
                    type="button"
                    onClick={() => onRemove(item.id)}
                    disabled={removing}
                    aria-label={`Remove ${item.name}`}
                    className="rounded-sm text-muted-foreground hover:text-foreground disabled:opacity-50"
                  >
                    {removing ? <Spinner size="sm" /> : <X className="h-3 w-3" />}
                  </button>
                </Badge>
              </li>
            )
          })}
        </ul>
      ) : (
        <p className="text-sm text-muted-foreground">No {plural} added yet.</p>
      )}

      {picker ?? (
        <div className="flex items-center gap-2">
          <Select value={selected} onValueChange={setSelected} disabled={optionsLoading}>
            <SelectTrigger className="w-56" aria-label={`Select a ${noun} to add`}>
              <SelectValue
                placeholder={
                  optionsLoading
                    ? 'Loading…'
                    : options.length === 0
                      ? `No ${plural} available`
                      : available.length === 0
                        ? `All ${plural} added`
                        : `Add a ${noun}…`
                }
              />
            </SelectTrigger>
            <SelectContent>
              {available.map((opt) => (
                <SelectItem key={opt.id} value={opt.id}>
                  {opt.name}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={handleAdd}
            disabled={!selected || addPending}
          >
            {addPending ? <Spinner size="sm" className="mr-1" /> : <Plus className="h-4 w-4" />}
            Add
          </Button>
        </div>
      )}
    </div>
  )
}
