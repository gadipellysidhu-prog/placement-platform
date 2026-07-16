import { useState } from 'react'
import { CalendarRange, Pencil, Plus, RefreshCw } from 'lucide-react'
import { PageHeader } from '@/shared/ui/page-header'
import { DataTable, type Column } from '@/shared/ui/data-table'
import { Badge } from '@/shared/ui/badge'
import { Button } from '@/shared/ui/button'
import { ConfirmDialog } from '@/features/job-postings/components/ConfirmDialog'
import { useToast } from '@/shared/hooks/use-toast'
import { getApiErrorMessage } from '@/lib/api'
import { formatDate } from '@/utils/format'
import type { AcademicYearResponse, CreateAcademicYearRequest } from '@/lib/api'
import {
  useAcademicYears,
  useActivateAcademicYear,
  useCreateAcademicYear,
  useUpdateAcademicYear,
} from '../hooks/use-academic-years'
import { AcademicYearFormDialog } from '../components/AcademicYearFormDialog'

const PAGE_SIZE = 20

export default function AcademicYearsPage() {
  const [page, setPage] = useState(0)
  const [formOpen, setFormOpen] = useState(false)
  const [editing, setEditing] = useState<AcademicYearResponse | null>(null)
  const [activating, setActivating] = useState<AcademicYearResponse | null>(null)

  const toast = useToast()

  const { data, isLoading, isError, refetch, isFetching } = useAcademicYears({
    page,
    size: PAGE_SIZE,
  })

  const create = useCreateAcademicYear()
  const update = useUpdateAcademicYear(editing?.id ?? '')
  const activate = useActivateAcademicYear()

  const currentActive = (data?.content ?? []).find((y) => y.active)

  function openCreate() {
    setEditing(null)
    setFormOpen(true)
  }

  function handleSave(payload: CreateAcademicYearRequest) {
    if (editing) {
      update.mutate(
        { startDate: payload.startDate, endDate: payload.endDate },
        {
          onSuccess: () => {
            setFormOpen(false)
            toast.success('Academic year updated.')
          },
          onError: (err) => toast.error('Could not save academic year', getApiErrorMessage(err)),
        },
      )
      return
    }
    create.mutate(payload, {
      onSuccess: () => {
        setFormOpen(false)
        toast.success('Academic year created.')
      },
      // A duplicate label or invalid range is refused by the backend.
      onError: (err) => toast.error('Could not save academic year', getApiErrorMessage(err)),
    })
  }

  function handleActivate() {
    if (!activating) return
    activate.mutate(activating.id, {
      onSuccess: () => {
        setActivating(null)
        toast.success(`${activating.label} is now the active year.`)
      },
      onError: (err) => {
        setActivating(null)
        toast.error('Could not activate academic year', getApiErrorMessage(err))
      },
    })
  }

  const columns: Column<AcademicYearResponse>[] = [
    {
      key: 'label',
      header: 'Year',
      cell: (row) => <span className="font-medium">{row.label}</span>,
    },
    {
      key: 'range',
      header: 'Range',
      cell: (row) => `${formatDate(row.startDate)} — ${formatDate(row.endDate)}`,
    },
    {
      key: 'active',
      header: 'Status',
      cell: (row) =>
        row.active ? (
          <Badge variant="success">Active</Badge>
        ) : (
          <span className="text-muted-foreground">—</span>
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
            aria-label={`Edit ${row.label}`}
            onClick={() => {
              setEditing(row)
              setFormOpen(true)
            }}
          >
            <Pencil className="h-4 w-4" />
          </Button>
          {!row.active && (
            <Button variant="outline" size="sm" onClick={() => setActivating(row)}>
              Activate
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
        title="Academic Years"
        description="Placement seasons. Exactly one year is active at a time."
        action={
          <div className="flex items-center gap-2">
            <Button variant="outline" onClick={() => void refetch()} disabled={isFetching}>
              <RefreshCw className={isFetching ? 'h-4 w-4 animate-spin' : 'h-4 w-4'} />
              Refresh
            </Button>
            <Button onClick={openCreate}>
              <Plus className="h-4 w-4" /> New year
            </Button>
          </div>
        }
      />

      <DataTable
        columns={columns}
        data={data?.content ?? []}
        isLoading={isLoading}
        isError={isError}
        onRetry={() => void refetch()}
        emptyTitle="No academic years yet"
        emptyDescription="Create a year to start a placement season."
        emptyIcon={<CalendarRange />}
        emptyAction={
          <Button variant="outline" size="sm" onClick={openCreate}>
            <Plus className="h-4 w-4" /> New year
          </Button>
        }
        page={page}
        totalPages={data?.totalPages ?? 1}
        onPageChange={setPage}
        getRowKey={(row) => row.id}
      />

      <AcademicYearFormDialog
        year={editing}
        open={formOpen}
        onOpenChange={setFormOpen}
        onConfirm={handleSave}
        isPending={create.isPending || update.isPending}
      />

      <ConfirmDialog
        open={activating !== null}
        onOpenChange={(next) => !next && setActivating(null)}
        title="Switch the active year?"
        description={
          // The backend enforces a single active year, so activating one deactivates
          // the other. Name it so the switch is never a surprise.
          currentActive && activating
            ? `${activating.label} becomes the active placement season, replacing ${currentActive.label}.`
            : activating
              ? `${activating.label} becomes the active placement season.`
              : ''
        }
        confirmLabel="Activate year"
        isPending={activate.isPending}
        onConfirm={handleActivate}
      />
    </div>
  )
}
