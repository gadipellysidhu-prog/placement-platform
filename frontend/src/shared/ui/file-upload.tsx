import * as React from 'react'
import { Upload, X, FileText, AlertCircle } from 'lucide-react'
import { cn } from '@/utils/cn'
import { Progress } from './progress'
import { Button } from './button'

interface FileUploadProps {
  accept?: string
  maxSizeBytes?: number
  onFile: (file: File) => void
  uploading?: boolean
  progress?: number
  error?: string
  className?: string
  disabled?: boolean
}

function FileUpload({
  accept = 'application/pdf,image/png,image/jpeg',
  maxSizeBytes = 10 * 1024 * 1024,
  onFile,
  uploading = false,
  progress,
  error,
  className,
  disabled,
}: FileUploadProps) {
  const [dragging, setDragging] = React.useState(false)
  const [selectedFile, setSelectedFile] = React.useState<File | null>(null)
  const [validationError, setValidationError] = React.useState<string | null>(null)
  const inputRef = React.useRef<HTMLInputElement>(null)

  function validate(file: File): string | null {
    if (maxSizeBytes && file.size > maxSizeBytes) {
      return `File exceeds maximum size of ${Math.round(maxSizeBytes / 1024 / 1024)}MB`
    }
    const allowed = accept.split(',').map((t) => t.trim())
    if (!allowed.some((t) => file.type === t || file.name.endsWith(t.replace('*', '')))) {
      return `File type not allowed. Accepted: ${accept}`
    }
    return null
  }

  function handleFile(file: File) {
    const err = validate(file)
    if (err) {
      setValidationError(err)
      return
    }
    setValidationError(null)
    setSelectedFile(file)
    onFile(file)
  }

  function onDrop(e: React.DragEvent) {
    e.preventDefault()
    setDragging(false)
    if (disabled || uploading) return
    const file = e.dataTransfer.files[0]
    if (file) handleFile(file)
  }

  const displayError = error ?? validationError

  return (
    <div className={cn('space-y-3', className)}>
      <div
        role="button"
        tabIndex={disabled || uploading ? -1 : 0}
        aria-label="Upload file"
        aria-disabled={disabled || uploading}
        onDragOver={(e) => {
          e.preventDefault()
          if (!disabled && !uploading) setDragging(true)
        }}
        onDragLeave={() => setDragging(false)}
        onDrop={onDrop}
        onClick={() => !disabled && !uploading && inputRef.current?.click()}
        onKeyDown={(e) => {
          if (e.key === 'Enter' || e.key === ' ') inputRef.current?.click()
        }}
        className={cn(
          'flex cursor-pointer flex-col items-center justify-center gap-3 rounded-xl border-2 border-dashed p-8 text-center transition-colors',
          dragging
            ? 'border-primary bg-primary/5'
            : 'border-border hover:border-primary/50 hover:bg-accent/30',
          (disabled || uploading) && 'cursor-not-allowed opacity-50',
          displayError && 'border-destructive',
        )}
      >
        <Upload className="h-8 w-8 text-muted-foreground" aria-hidden="true" />
        <div className="space-y-1">
          <p className="text-sm font-medium text-foreground">
            {dragging ? 'Drop file here' : 'Drag & drop or click to upload'}
          </p>
          <p className="text-xs text-muted-foreground">
            Accepted: {accept} · Max {Math.round(maxSizeBytes / 1024 / 1024)}MB
          </p>
        </div>
        <input
          ref={inputRef}
          type="file"
          accept={accept}
          className="sr-only"
          onChange={(e) => {
            const f = e.target.files?.[0]
            if (f) handleFile(f)
          }}
          disabled={disabled || uploading}
          aria-hidden="true"
          tabIndex={-1}
        />
      </div>

      {selectedFile && !displayError && (
        <div className="flex items-center gap-3 rounded-lg border border-border p-3">
          <FileText className="h-5 w-5 shrink-0 text-muted-foreground" aria-hidden="true" />
          <div className="min-w-0 flex-1">
            <p className="truncate text-sm font-medium text-foreground">{selectedFile.name}</p>
            <p className="text-xs text-muted-foreground">
              {(selectedFile.size / 1024).toFixed(1)} KB
            </p>
            {uploading && progress !== undefined && (
              <Progress value={progress} className="mt-2 h-1" />
            )}
          </div>
          {!uploading && (
            <Button
              type="button"
              variant="ghost"
              size="icon"
              onClick={(e) => {
                e.stopPropagation()
                setSelectedFile(null)
              }}
              aria-label="Remove file"
            >
              <X className="h-4 w-4" />
            </Button>
          )}
        </div>
      )}

      {displayError && (
        <p role="alert" className="flex items-center gap-2 text-sm text-destructive">
          <AlertCircle className="h-4 w-4 shrink-0" aria-hidden="true" />
          {displayError}
        </p>
      )}
    </div>
  )
}

export { FileUpload }
