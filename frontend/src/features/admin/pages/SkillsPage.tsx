import { useMemo, useState } from 'react'
import { BadgeCheck, Pencil, Plus, RefreshCw, Sparkles } from 'lucide-react'
import { PageHeader } from '@/shared/ui/page-header'
import { DataTable, type Column } from '@/shared/ui/data-table'
import { Badge } from '@/shared/ui/badge'
import { Button } from '@/shared/ui/button'
import { SearchInput } from '@/shared/ui/search-input'
import { ConfirmDialog } from '@/features/job-postings/components/ConfirmDialog'
import { useDebounce } from '@/shared/hooks/use-debounce'
import { useToast } from '@/shared/hooks/use-toast'
import { getApiErrorMessage, skillsApi, queryKeys } from '@/lib/api'
import { useQuery } from '@tanstack/react-query'
import type { CreateSkillRequest, SkillResponse } from '@/lib/api'
import {
  useCreateSkill,
  useSkills,
  useUpdateSkill,
  useVerifySkill,
} from '../hooks/use-skills-admin'
import { SkillFormDialog } from '../components/SkillFormDialog'

const SEARCH_LIMIT = 50

export default function SkillsPage() {
  const [search, setSearch] = useState('')
  const [formOpen, setFormOpen] = useState(false)
  const [editing, setEditing] = useState<SkillResponse | null>(null)
  const [verifying, setVerifying] = useState<SkillResponse | null>(null)

  const toast = useToast()
  const debouncedSearch = useDebounce(search)
  const term = debouncedSearch.trim()

  const listQuery = useSkills()
  const create = useCreateSkill()
  const update = useUpdateSkill(editing?.id ?? '')
  const verify = useVerifySkill()

  /**
   * Search goes through the catalog's own ranked endpoint (names, aliases,
   * abbreviations, fuzzy) rather than filtering the list client-side — the backend
   * ranks far better than a substring match, and it is what the tagging picker uses.
   */
  const searchQuery = useQuery({
    queryKey: queryKeys.skills.search(term),
    queryFn: () => skillsApi.search(term, SEARCH_LIMIT),
    enabled: term.length > 0,
    staleTime: 30_000,
  })

  const isSearching = term.length > 0
  const activeQuery = isSearching ? searchQuery : listQuery

  // Search returns a ranked projection, so rows are keyed back to the full catalog
  // entries to keep the verified/active columns and the row actions meaningful.
  const rows = useMemo<SkillResponse[]>(() => {
    if (!isSearching) return listQuery.data ?? []
    const byId = new Map((listQuery.data ?? []).map((s) => [s.id, s]))
    return (searchQuery.data ?? [])
      .map((hit) => byId.get(hit.id))
      .filter((s): s is SkillResponse => s !== undefined)
  }, [isSearching, listQuery.data, searchQuery.data])

  function openCreate() {
    setEditing(null)
    setFormOpen(true)
  }

  function handleSave(payload: CreateSkillRequest) {
    const mutation = editing ? update : create
    mutation.mutate(payload, {
      onSuccess: () => {
        setFormOpen(false)
        toast.success(editing ? 'Skill updated.' : 'Skill created.')
      },
      // A duplicate name is refused with a 409 worded by the backend.
      onError: (err) => toast.error('Could not save skill', getApiErrorMessage(err)),
    })
  }

  function handleVerify() {
    if (!verifying) return
    verify.mutate(verifying.id, {
      onSuccess: () => {
        setVerifying(null)
        toast.success('Skill verified.')
      },
      onError: (err) => {
        setVerifying(null)
        toast.error('Could not verify skill', getApiErrorMessage(err))
      },
    })
  }

  const columns: Column<SkillResponse>[] = [
    {
      key: 'name',
      header: 'Name',
      cell: (row) => <span className="font-medium">{row.name}</span>,
    },
    {
      key: 'category',
      header: 'Category',
      cell: (row) => row.category ?? <span className="text-muted-foreground">—</span>,
      className: 'w-40',
    },
    {
      key: 'source',
      header: 'Source',
      cell: (row) => <Badge variant="outline">{row.createdSource}</Badge>,
      className: 'w-28',
    },
    {
      key: 'verified',
      header: 'Status',
      cell: (row) => (
        <Badge variant={row.verified ? 'success' : 'secondary'}>
          {row.verified ? 'Verified' : 'Unverified'}
        </Badge>
      ),
      className: 'w-32',
    },
    {
      key: 'actions',
      header: <span className="sr-only">Actions</span>,
      cell: (row) => (
        <div className="flex justify-end gap-1">
          <Button
            variant="ghost"
            size="sm"
            aria-label={`Edit ${row.name}`}
            onClick={() => {
              setEditing(row)
              setFormOpen(true)
            }}
          >
            <Pencil className="h-4 w-4" />
          </Button>
          {!row.verified && (
            <Button variant="outline" size="sm" onClick={() => setVerifying(row)}>
              <BadgeCheck className="h-4 w-4" /> Verify
            </Button>
          )}
        </div>
      ),
      className: 'w-40',
    },
  ]

  return (
    <div className="space-y-6">
      <PageHeader
        title="Skills"
        description="The master skills catalogue used for tagging and matching"
        action={
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              onClick={() => void activeQuery.refetch()}
              disabled={activeQuery.isFetching}
            >
              <RefreshCw className={activeQuery.isFetching ? 'h-4 w-4 animate-spin' : 'h-4 w-4'} />
              Refresh
            </Button>
            <Button onClick={openCreate}>
              <Plus className="h-4 w-4" /> New skill
            </Button>
          </div>
        }
      />

      <SearchInput
        className="w-full sm:w-72"
        placeholder="Search names, aliases, abbreviations…"
        aria-label="Search skills"
        value={search}
        onChange={(e) => setSearch(e.target.value)}
        onClear={() => setSearch('')}
      />

      <DataTable
        columns={columns}
        data={rows}
        isLoading={activeQuery.isLoading}
        isError={activeQuery.isError}
        onRetry={() => void activeQuery.refetch()}
        emptyTitle={isSearching ? 'No matching skills' : 'No skills yet'}
        emptyDescription={isSearching ? 'Try a different term.' : 'Create a skill to get started.'}
        emptyIcon={<Sparkles />}
        emptyAction={
          !isSearching ? (
            <Button variant="outline" size="sm" onClick={openCreate}>
              <Plus className="h-4 w-4" /> New skill
            </Button>
          ) : undefined
        }
        getRowKey={(row) => row.id}
      />

      <SkillFormDialog
        skill={editing}
        open={formOpen}
        onOpenChange={setFormOpen}
        onConfirm={handleSave}
        isPending={create.isPending || update.isPending}
      />

      <ConfirmDialog
        open={verifying !== null}
        onOpenChange={(next) => !next && setVerifying(null)}
        title="Verify skill?"
        description={
          verifying ? `"${verifying.name}" will be marked as a verified catalogue entry.` : ''
        }
        confirmLabel="Verify skill"
        isPending={verify.isPending}
        onConfirm={handleVerify}
      />
    </div>
  )
}
