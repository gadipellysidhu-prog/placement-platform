import { useEffect, useId, useMemo, useRef, useState } from 'react'
import { Plus } from 'lucide-react'
import { SearchInput } from '@/shared/ui/search-input'
import { Spinner } from '@/shared/ui/spinner'
import { cn } from '@/utils/cn'
import type { SkillSearchResult } from '@/lib/api'
import { useSkillSearch } from '../hooks/use-skill-search'

interface SkillSearchPickerProps {
  /** Skills already attached to the posting — hidden from results. */
  attachedIds: Set<string>
  addPending?: boolean
  onAdd: (skillId: string) => void
}

/**
 * Intelligent skill picker: a debounced search box over the catalog search
 * endpoint (names, aliases, abbreviations, fuzzy — server-ranked), results
 * grouped by category. Implements the WAI-ARIA combobox pattern: ArrowUp/Down
 * move the active option, Enter adds it, Escape closes.
 */
export function SkillSearchPicker({ attachedIds, addPending, onAdd }: SkillSearchPickerProps) {
  const [query, setQuery] = useState('')
  const [open, setOpen] = useState(false)
  const [activeIndex, setActiveIndex] = useState(0)
  const listboxId = useId()
  const containerRef = useRef<HTMLDivElement>(null)

  const { data, isFetching } = useSkillSearch(query)

  const results = useMemo(
    () => (data ?? []).filter((r) => !attachedIds.has(r.id)),
    [data, attachedIds],
  )

  /** Results grouped by category, preserving the server's ranking order. */
  const grouped = useMemo(() => {
    const groups = new Map<string, SkillSearchResult[]>()
    for (const result of results) {
      const category = result.category ?? 'Other'
      const bucket = groups.get(category) ?? []
      bucket.push(result)
      groups.set(category, bucket)
    }
    return [...groups.entries()]
  }, [results])

  useEffect(() => {
    setActiveIndex(0)
  }, [results])

  // Close when focus/click leaves the picker.
  useEffect(() => {
    function onPointerDown(event: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setOpen(false)
      }
    }
    document.addEventListener('mousedown', onPointerDown)
    return () => document.removeEventListener('mousedown', onPointerDown)
  }, [])

  function add(result: SkillSearchResult) {
    onAdd(result.id)
    setQuery('')
    setOpen(false)
  }

  function onKeyDown(event: React.KeyboardEvent<HTMLInputElement>) {
    if (!open || results.length === 0) {
      if (event.key === 'ArrowDown' && results.length > 0) {
        setOpen(true)
        event.preventDefault()
      }
      return
    }
    switch (event.key) {
      case 'ArrowDown':
        event.preventDefault()
        setActiveIndex((i) => Math.min(i + 1, results.length - 1))
        break
      case 'ArrowUp':
        event.preventDefault()
        setActiveIndex((i) => Math.max(i - 1, 0))
        break
      case 'Enter':
        event.preventDefault()
        if (results[activeIndex]) {
          add(results[activeIndex])
        }
        break
      case 'Escape':
        setOpen(false)
        break
    }
  }

  const activeId = results[activeIndex] ? `${listboxId}-opt-${results[activeIndex].id}` : undefined
  const showList = open && query.trim().length > 0

  return (
    <div ref={containerRef} className="relative w-72 max-w-full">
      <SearchInput
        role="combobox"
        aria-expanded={showList}
        aria-controls={listboxId}
        aria-activedescendant={activeId}
        aria-label="Search skills to add"
        aria-autocomplete="list"
        placeholder="Search skills (e.g. reactjs, ml)…"
        value={query}
        disabled={addPending}
        onChange={(e) => {
          setQuery(e.target.value)
          setOpen(true)
        }}
        onFocus={() => setOpen(true)}
        onKeyDown={onKeyDown}
        onClear={() => {
          setQuery('')
          setOpen(false)
        }}
      />
      {addPending && (
        <span className="absolute -right-7 top-2.5">
          <Spinner size="sm" />
        </span>
      )}

      {showList && (
        <ul
          id={listboxId}
          role="listbox"
          aria-label="Skill search results"
          className="absolute z-30 mt-1 max-h-72 w-full overflow-y-auto rounded-md border border-border bg-popover p-1 text-popover-foreground shadow-md"
        >
          {results.length === 0 ? (
            <li className="px-2 py-3 text-sm text-muted-foreground" role="presentation">
              {isFetching ? 'Searching…' : 'No matching skills'}
            </li>
          ) : (
            grouped.map(([category, group]) => (
              <li key={category} role="presentation">
                <div className="px-2 pb-1 pt-2 text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                  {category}
                </div>
                <ul role="presentation">
                  {group.map((result) => {
                    const index = results.indexOf(result)
                    const isActive = index === activeIndex
                    return (
                      <li
                        key={result.id}
                        id={`${listboxId}-opt-${result.id}`}
                        role="option"
                        aria-selected={isActive}
                        className={cn(
                          'flex cursor-pointer items-center justify-between gap-2 rounded-sm px-2 py-1.5 text-sm',
                          isActive ? 'bg-accent text-accent-foreground' : 'hover:bg-accent/60',
                        )}
                        onMouseEnter={() => setActiveIndex(index)}
                        onMouseDown={(e) => {
                          // mousedown (not click) so the input never loses focus first
                          e.preventDefault()
                          add(result)
                        }}
                      >
                        <span className="truncate">{result.name}</span>
                        <span className="flex shrink-0 items-center gap-1 text-xs text-muted-foreground">
                          {result.matchType === 'ALIAS' && <span>alias</span>}
                          {result.matchType === 'FUZZY' && <span>similar</span>}
                          <Plus className="h-3 w-3" aria-hidden="true" />
                        </span>
                      </li>
                    )
                  })}
                </ul>
              </li>
            ))
          )}
        </ul>
      )}
    </div>
  )
}
