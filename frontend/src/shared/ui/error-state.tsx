import * as React from 'react'
import { AlertTriangle, RefreshCw } from 'lucide-react'
import { Button } from './button'
import { cn } from '@/utils/cn'

interface ErrorStateProps extends React.HTMLAttributes<HTMLDivElement> {
  title?: string
  description?: string
  onRetry?: () => void
  retryLabel?: string
}

function ErrorState({
  title = 'Something went wrong',
  description = 'An unexpected error occurred. Please try again.',
  onRetry,
  retryLabel = 'Try again',
  className,
  ...props
}: ErrorStateProps) {
  return (
    <div
      role="alert"
      className={cn(
        'flex flex-col items-center justify-center gap-4 rounded-xl border border-dashed border-destructive/50 p-12 text-center',
        className,
      )}
      {...props}
    >
      <AlertTriangle className="h-10 w-10 text-destructive" aria-hidden="true" />
      <div className="space-y-1">
        <h3 className="text-sm font-medium text-foreground">{title}</h3>
        {description && <p className="text-sm text-muted-foreground">{description}</p>}
      </div>
      {onRetry && (
        <Button variant="outline" size="sm" onClick={onRetry}>
          <RefreshCw className="h-4 w-4" />
          {retryLabel}
        </Button>
      )}
    </div>
  )
}

export { ErrorState }
