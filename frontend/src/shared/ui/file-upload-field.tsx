import * as React from 'react'
import {
  FileText,
  CheckCircle2,
  ShieldAlert,
  RefreshCw,
  Replace as ReplaceIcon,
  X,
} from 'lucide-react'
import { cn } from '@/utils/cn'
import { formatFileSize } from '@/utils/format'
import { filesApi, type FileResponse } from '@/lib/api'
import { useFileUpload } from '@/shared/hooks/use-file-upload'
import { FileUpload } from './file-upload'
import { Button } from './button'
import { Badge } from './badge'

interface FileUploadFieldProps {
  /** The currently uploaded file's metadata, or null when none is attached. */
  value: FileResponse | null
  /** Called with the uploaded file metadata (or null when cleared). */
  onChange: (file: FileResponse | null) => void
  accept?: string
  maxSizeBytes?: number
  disabled?: boolean
  className?: string
  /**
   * Best-effort cleanup of the previous upload via DELETE /api/files/{id} when the
   * file is replaced or removed. The backend restricts deletion to ADMIN /
   * PLACEMENT_OFFICER, so a student's cleanup call may 403 — that is swallowed
   * (the orphaned file is harmless and reclaimable by an operator).
   */
  cleanupOnReplace?: boolean
}

/** Small inline indicator for the backend virus-scan outcome of a stored file. */
function ScanStatusBadge({ status }: { status: FileResponse['scanStatus'] }) {
  switch (status) {
    case 'CLEAN':
      return <Badge variant="success">Scanned clean</Badge>
    case 'PENDING':
      return <Badge variant="warning">Scan pending</Badge>
    case 'FAILED':
      return <Badge variant="secondary">Scan unavailable</Badge>
    case 'INFECTED':
      return <Badge variant="destructive">Infected</Badge>
    default:
      return null
  }
}

/**
 * Reusable, self-contained upload control that drives the full file lifecycle against
 * the backend file pipeline: pick/drag → upload (progress) → success metadata, plus
 * replace, remove, and retry. Validation, virus-scan rejection, and any other backend
 * ProblemDetail are surfaced inline. The uploaded {@link FileResponse} is handed back to
 * the parent through `onChange`.
 */
export function FileUploadField({
  value,
  onChange,
  accept = 'application/pdf,image/png,image/jpeg',
  maxSizeBytes = 10 * 1024 * 1024,
  disabled,
  className,
  cleanupOnReplace = true,
}: FileUploadFieldProps) {
  const { upload, uploading, progress, error, reset } = useFileUpload()
  const [lastFile, setLastFile] = React.useState<File | null>(null)

  async function handleFile(file: File) {
    setLastFile(file)
    const result = await upload(file)
    if (result) {
      onChange(result)
      reset()
    }
    // On failure, `error` from the hook renders inline with a retry affordance.
  }

  async function cleanup(id: string) {
    if (!cleanupOnReplace) return
    try {
      await filesApi.delete(id)
    } catch {
      // Best-effort only — see cleanupOnReplace docs.
    }
  }

  async function handleRemove() {
    if (value) await cleanup(value.id)
    onChange(null)
    setLastFile(null)
    reset()
  }

  async function handleReplace() {
    if (value) await cleanup(value.id)
    onChange(null)
    reset()
  }

  // ── Success state: file attached ─────────────────────────────────────────
  if (value && !uploading) {
    return (
      <div
        className={cn('flex items-center gap-3 rounded-lg border border-border p-3', className)}
        data-testid="file-upload-success"
      >
        <FileText className="h-5 w-5 shrink-0 text-muted-foreground" aria-hidden="true" />
        <div className="min-w-0 flex-1 space-y-1">
          <p className="truncate text-sm font-medium text-foreground">{value.filename}</p>
          <div className="flex flex-wrap items-center gap-2">
            <span className="text-xs text-muted-foreground">{formatFileSize(value.sizeBytes)}</span>
            <ScanStatusBadge status={value.scanStatus} />
          </div>
        </div>
        <div className="flex shrink-0 items-center gap-1">
          <Button
            type="button"
            variant="ghost"
            size="sm"
            onClick={handleReplace}
            disabled={disabled}
          >
            <ReplaceIcon className="h-4 w-4" /> Replace
          </Button>
          <Button
            type="button"
            variant="ghost"
            size="icon"
            onClick={handleRemove}
            disabled={disabled}
            aria-label="Remove file"
          >
            <X className="h-4 w-4" />
          </Button>
        </div>
      </div>
    )
  }

  // ── Idle / uploading / error state ───────────────────────────────────────
  return (
    <div className={cn('space-y-3', className)}>
      <FileUpload
        accept={accept}
        maxSizeBytes={maxSizeBytes}
        onFile={handleFile}
        uploading={uploading}
        progress={progress}
        error={error ?? undefined}
        disabled={disabled}
      />

      {/* The upload error message is rendered inline by FileUpload; here we only add a
          scan-rejection hint icon and a retry affordance for the last picked file. */}
      {error && lastFile && (
        <div className="flex items-center justify-between gap-3 rounded-lg border border-destructive/40 bg-destructive/5 p-3">
          <span className="flex items-center gap-2 text-xs text-muted-foreground">
            <ShieldAlert className="h-4 w-4 shrink-0 text-destructive" aria-hidden="true" />
            Upload rejected — you can try again.
          </span>
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={() => handleFile(lastFile)}
            disabled={disabled || uploading}
          >
            <RefreshCw className="h-4 w-4" /> Retry
          </Button>
        </div>
      )}

      {uploading && (
        <p className="flex items-center gap-2 text-xs text-muted-foreground">
          <CheckCircle2 className="h-3.5 w-3.5" aria-hidden="true" /> Uploading &amp; scanning…{' '}
          {progress}%
        </p>
      )}
    </div>
  )
}
