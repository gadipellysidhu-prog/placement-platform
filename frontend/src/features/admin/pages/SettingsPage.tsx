import { useEffect, useMemo, useState } from 'react'
import { Pencil, Plus, RefreshCw, Settings2, Trash2 } from 'lucide-react'
import { PageHeader } from '@/shared/ui/page-header'
import { DataTable, type Column } from '@/shared/ui/data-table'
import { Badge } from '@/shared/ui/badge'
import { Button } from '@/shared/ui/button'
import { SearchInput } from '@/shared/ui/search-input'
import { ConfirmDialog } from '@/shared/ui/confirm-dialog'
import { useDebounce } from '@/shared/hooks/use-debounce'
import { useToast } from '@/shared/hooks/use-toast'
import { getApiErrorMessage } from '@/lib/api'
import { truncate } from '@/utils/format'
import type { SettingResponse, SettingUpsertRequest } from '@/lib/api'
import { useDeleteSetting, useSettings, useUpsertSetting } from '../hooks/use-settings'
import { SettingFormDialog } from '../components/SettingFormDialog'

const PAGE_SIZE = 20
const MAX_VALUE_PREVIEW = 60

export default function SettingsPage() {
  const [page, setPage] = useState(0)
  const [category, setCategory] = useState('')
  const [formOpen, setFormOpen] = useState(false)
  const [editing, setEditing] = useState<SettingResponse | null>(null)
  const [deleting, setDeleting] = useState<SettingResponse | null>(null)

  const toast = useToast()
  const debouncedCategory = useDebounce(category)

  const params = useMemo(
    () => ({ page, size: PAGE_SIZE, category: debouncedCategory.trim() || undefined }),
    [page, debouncedCategory],
  )

  useEffect(() => {
    setPage(0)
  }, [debouncedCategory])

  const { data, isLoading, isError, refetch, isFetching } = useSettings(params)
  const upsert = useUpsertSetting()
  const remove = useDeleteSetting()

  function openCreate() {
    setEditing(null)
    setFormOpen(true)
  }

  function openEdit(setting: SettingResponse) {
    setEditing(setting)
    setFormOpen(true)
  }

  function handleSave(payload: SettingUpsertRequest) {
    upsert.mutate(payload, {
      onSuccess: () => {
        setFormOpen(false)
        toast.success(editing ? 'Setting updated.' : 'Setting created.')
      },
      onError: (err) => toast.error('Could not save setting', getApiErrorMessage(err)),
    })
  }

  function handleDelete() {
    if (!deleting) return
    remove.mutate(deleting.id, {
      onSuccess: () => {
        setDeleting(null)
        toast.success('Setting deleted.')
      },
      onError: (err) => {
        setDeleting(null)
        toast.error('Could not delete setting', getApiErrorMessage(err))
      },
    })
  }

  const columns: Column<SettingResponse>[] = [
    {
      key: 'key',
      header: 'Key',
      cell: (row) => (
        <div className="space-y-0.5">
          <div className="font-medium">{row.settingKey}</div>
          {row.description && (
            <div className="text-xs text-muted-foreground">{row.description}</div>
          )}
        </div>
      ),
    },
    {
      key: 'value',
      header: 'Value',
      cell: (row) =>
        row.settingValue ? (
          <span className="font-mono text-xs">{truncate(row.settingValue, MAX_VALUE_PREVIEW)}</span>
        ) : (
          <span className="text-muted-foreground">—</span>
        ),
    },
    {
      key: 'type',
      header: 'Type',
      cell: (row) => <Badge variant="outline">{row.valueType}</Badge>,
      className: 'w-28',
    },
    {
      key: 'category',
      header: 'Category',
      cell: (row) => row.category ?? <span className="text-muted-foreground">—</span>,
      className: 'w-36',
    },
    {
      key: 'scope',
      header: 'Scope',
      cell: (row) => (row.academicYearId ? 'Academic year' : 'Global'),
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
            aria-label={`Edit ${row.settingKey}`}
            onClick={() => openEdit(row)}
          >
            <Pencil className="h-4 w-4" />
          </Button>
          <Button
            variant="ghost"
            size="sm"
            aria-label={`Delete ${row.settingKey}`}
            onClick={() => setDeleting(row)}
          >
            <Trash2 className="h-4 w-4 text-destructive" />
          </Button>
        </div>
      ),
      className: 'w-24',
    },
  ]

  return (
    <div className="space-y-6">
      <PageHeader
        title="Settings"
        description="Configuration-driven application settings"
        action={
          <div className="flex items-center gap-2">
            <Button variant="outline" onClick={() => void refetch()} disabled={isFetching}>
              <RefreshCw className={isFetching ? 'h-4 w-4 animate-spin' : 'h-4 w-4'} />
              Refresh
            </Button>
            <Button onClick={openCreate}>
              <Plus className="h-4 w-4" /> New setting
            </Button>
          </div>
        }
      />

      <SearchInput
        className="w-full sm:w-72"
        placeholder="Filter by category…"
        aria-label="Filter settings by category"
        value={category}
        onChange={(e) => setCategory(e.target.value)}
        onClear={() => setCategory('')}
      />

      <DataTable
        columns={columns}
        data={data?.content ?? []}
        isLoading={isLoading}
        isError={isError}
        onRetry={() => void refetch()}
        emptyTitle={params.category ? 'No settings in this category' : 'No settings yet'}
        emptyDescription={
          params.category ? 'Try a different category.' : 'Create a setting to get started.'
        }
        emptyIcon={<Settings2 />}
        emptyAction={
          !params.category ? (
            <Button variant="outline" size="sm" onClick={openCreate}>
              <Plus className="h-4 w-4" /> New setting
            </Button>
          ) : undefined
        }
        page={page}
        totalPages={data?.totalPages ?? 1}
        onPageChange={setPage}
        getRowKey={(row) => row.id}
      />

      <SettingFormDialog
        setting={editing}
        open={formOpen}
        onOpenChange={setFormOpen}
        onConfirm={handleSave}
        isPending={upsert.isPending}
      />

      <ConfirmDialog
        open={deleting !== null}
        onOpenChange={(next) => !next && setDeleting(null)}
        title="Delete setting?"
        description={
          deleting
            ? `"${deleting.settingKey}" will be removed. Anything relying on it falls back to its built-in default.`
            : ''
        }
        confirmLabel="Delete"
        destructive
        isPending={remove.isPending}
        onConfirm={handleDelete}
      />
    </div>
  )
}
