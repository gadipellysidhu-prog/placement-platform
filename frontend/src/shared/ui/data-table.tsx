import * as React from 'react'
import { Table, TableHeader, TableBody, TableHead, TableRow, TableCell } from './table'
import { Skeleton } from './skeleton'
import { Pagination } from './pagination'
import { EmptyState } from './empty-state'
import { ErrorState } from './error-state'

export interface Column<T> {
  key: string
  header: React.ReactNode
  cell: (row: T) => React.ReactNode
  className?: string
}

interface DataTableProps<T> {
  columns: Column<T>[]
  data: T[]
  isLoading?: boolean
  isError?: boolean
  onRetry?: () => void
  emptyTitle?: string
  emptyDescription?: string
  emptyIcon?: React.ReactNode
  emptyAction?: React.ReactNode
  /** 0-indexed current page */
  page?: number
  totalPages?: number
  onPageChange?: (page: number) => void
  getRowKey: (row: T) => string
  onRowClick?: (row: T) => void
}

const SKELETON_ROWS = 5

function DataTable<T>({
  columns,
  data,
  isLoading,
  isError,
  onRetry,
  emptyTitle = 'No results',
  emptyDescription,
  emptyIcon,
  emptyAction,
  page = 0,
  totalPages = 1,
  onPageChange,
  getRowKey,
  onRowClick,
}: DataTableProps<T>) {
  if (isError) {
    return (
      <ErrorState
        title="Failed to load data"
        description="An error occurred while fetching data."
        onRetry={onRetry}
        className="my-8"
      />
    )
  }

  return (
    <div className="space-y-4">
      <div className="rounded-lg border border-border overflow-hidden">
        <Table>
          <TableHeader>
            <TableRow className="hover:bg-transparent">
              {columns.map((col) => (
                <TableHead key={col.key} className={col.className}>
                  {col.header}
                </TableHead>
              ))}
            </TableRow>
          </TableHeader>
          <TableBody>
            {isLoading ? (
              Array.from({ length: SKELETON_ROWS }).map((_, i) => (
                <TableRow key={i} className="hover:bg-transparent">
                  {columns.map((col) => (
                    <TableCell key={col.key} className={col.className}>
                      <Skeleton className="h-4 w-full" />
                    </TableCell>
                  ))}
                </TableRow>
              ))
            ) : data.length === 0 ? (
              <TableRow className="hover:bg-transparent">
                <TableCell colSpan={columns.length} className="p-0">
                  <EmptyState
                    icon={emptyIcon}
                    title={emptyTitle}
                    description={emptyDescription}
                    action={emptyAction}
                    className="rounded-none border-0"
                  />
                </TableCell>
              </TableRow>
            ) : (
              data.map((row) => (
                <TableRow
                  key={getRowKey(row)}
                  onClick={onRowClick ? () => onRowClick(row) : undefined}
                  className={onRowClick ? 'cursor-pointer' : undefined}
                >
                  {columns.map((col) => (
                    <TableCell key={col.key} className={col.className}>
                      {col.cell(row)}
                    </TableCell>
                  ))}
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </div>

      {!isLoading && totalPages > 1 && onPageChange && (
        <Pagination page={page} totalPages={totalPages} onPageChange={onPageChange} />
      )}
    </div>
  )
}

export { DataTable }
