import { useMemo, useState } from 'react'
import { GitBranch, Pencil, Plus, RefreshCw } from 'lucide-react'
import { PageHeader } from '@/shared/ui/page-header'
import { DataTable, type Column } from '@/shared/ui/data-table'
import { Badge } from '@/shared/ui/badge'
import { Button } from '@/shared/ui/button'
import { SearchInput } from '@/shared/ui/search-input'
import { ConfirmDialog } from '@/shared/ui/confirm-dialog'
import { useToast } from '@/shared/hooks/use-toast'
import { getApiErrorMessage } from '@/lib/api'
import type { BranchResponse, CreateBranchRequest } from '@/lib/api'
import {
  useActivateBranch,
  useBranches,
  useCreateBranch,
  useDeactivateBranch,
  useUpdateBranch,
} from '../hooks/use-branches'
import { BranchFormDialog } from '../components/BranchFormDialog'

export default function BranchesPage() {
  const [search, setSearch] = useState('')
  const [formOpen, setFormOpen] = useState(false)
  const [editing, setEditing] = useState<BranchResponse | null>(null)
  const [toggling, setToggling] = useState<BranchResponse | null>(null)

  const toast = useToast()

  // The catalogue is small and the endpoint is unpaginated, so filtering is local.
  // activeOnly: false keeps deactivated branches reachable for reactivation.
  const { data, isLoading, isError, refetch, isFetching } = useBranches({ activeOnly: false })

  const create = useCreateBranch()
  const update = useUpdateBranch(editing?.id ?? '')
  const activate = useActivateBranch()
  const deactivate = useDeactivateBranch()

  const branches = useMemo(() => {
    const term = search.trim().toLowerCase()
    if (!term) return data ?? []
    return (data ?? []).filter(
      (b) => b.name.toLowerCase().includes(term) || (b.code ?? '').toLowerCase().includes(term),
    )
  }, [data, search])

  function openCreate() {
    setEditing(null)
    setFormOpen(true)
  }

  function handleSave(payload: CreateBranchRequest) {
    const mutation = editing ? update : create
    mutation.mutate(payload, {
      onSuccess: () => {
        setFormOpen(false)
        toast.success(editing ? 'Branch updated.' : 'Branch created.')
      },
      // A duplicate name/code is refused with a 409 the backend words itself.
      onError: (err) => toast.error('Could not save branch', getApiErrorMessage(err)),
    })
  }

  function handleToggleActive() {
    if (!toggling) return
    const mutation = toggling.active ? deactivate : activate
    const verb = toggling.active ? 'deactivated' : 'activated'
    mutation.mutate(toggling.id, {
      onSuccess: () => {
        setToggling(null)
        toast.success(`Branch ${verb}.`)
      },
      onError: (err) => {
        setToggling(null)
        toast.error(
          `Could not ${toggling.active ? 'deactivate' : 'activate'} branch`,
          getApiErrorMessage(err),
        )
      },
    })
  }

  const columns: Column<BranchResponse>[] = [
    {
      key: 'name',
      header: 'Name',
      cell: (row) => (
        <div className="space-y-0.5">
          <div className="font-medium">{row.name}</div>
          {row.description && (
            <div className="text-xs text-muted-foreground">{row.description}</div>
          )}
        </div>
      ),
    },
    {
      key: 'code',
      header: 'Code',
      cell: (row) => row.code ?? <span className="text-muted-foreground">—</span>,
      className: 'w-28',
    },
    {
      key: 'active',
      header: 'Status',
      cell: (row) => (
        <Badge variant={row.active ? 'success' : 'secondary'}>
          {row.active ? 'Active' : 'Inactive'}
        </Badge>
      ),
      className: 'w-28',
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
          <Button variant="outline" size="sm" onClick={() => setToggling(row)}>
            {row.active ? 'Deactivate' : 'Activate'}
          </Button>
        </div>
      ),
      className: 'w-44',
    },
  ]

  return (
    <div className="space-y-6">
      <PageHeader
        title="Branches"
        description="Academic branches used for student profiles and job eligibility"
        action={
          <div className="flex items-center gap-2">
            <Button variant="outline" onClick={() => void refetch()} disabled={isFetching}>
              <RefreshCw className={isFetching ? 'h-4 w-4 animate-spin' : 'h-4 w-4'} />
              Refresh
            </Button>
            <Button onClick={openCreate}>
              <Plus className="h-4 w-4" /> New branch
            </Button>
          </div>
        }
      />

      <SearchInput
        className="w-full sm:w-72"
        placeholder="Search branches…"
        aria-label="Search branches"
        value={search}
        onChange={(e) => setSearch(e.target.value)}
        onClear={() => setSearch('')}
      />

      <DataTable
        columns={columns}
        data={branches}
        isLoading={isLoading}
        isError={isError}
        onRetry={() => void refetch()}
        emptyTitle={search ? 'No matching branches' : 'No branches yet'}
        emptyDescription={search ? 'Try a different search.' : 'Create a branch to get started.'}
        emptyIcon={<GitBranch />}
        emptyAction={
          !search ? (
            <Button variant="outline" size="sm" onClick={openCreate}>
              <Plus className="h-4 w-4" /> New branch
            </Button>
          ) : undefined
        }
        getRowKey={(row) => row.id}
      />

      <BranchFormDialog
        branch={editing}
        open={formOpen}
        onOpenChange={setFormOpen}
        onConfirm={handleSave}
        isPending={create.isPending || update.isPending}
      />

      <ConfirmDialog
        open={toggling !== null}
        onOpenChange={(next) => !next && setToggling(null)}
        title={toggling?.active ? 'Deactivate branch?' : 'Activate branch?'}
        description={
          toggling?.active
            ? `"${toggling.name}" will stop appearing in student profiles and eligibility pickers. Existing records keep it.`
            : `"${toggling?.name}" will be selectable again.`
        }
        confirmLabel={toggling?.active ? 'Deactivate branch' : 'Activate branch'}
        destructive={toggling?.active}
        isPending={activate.isPending || deactivate.isPending}
        onConfirm={handleToggleActive}
      />
    </div>
  )
}
