import { Spinner } from './spinner'
import { cn } from '@/utils/cn'

interface LoadingOverlayProps {
  visible: boolean
  label?: string
  className?: string
}

function LoadingOverlay({ visible, label = 'Loading…', className }: LoadingOverlayProps) {
  if (!visible) return null

  return (
    <div
      className={cn(
        'absolute inset-0 z-10 flex items-center justify-center rounded-inherit bg-background/60 backdrop-blur-sm',
        className,
      )}
      aria-live="polite"
      aria-busy={visible}
    >
      <div className="flex flex-col items-center gap-3">
        <Spinner size="lg" label={label} />
        <p className="text-sm text-muted-foreground">{label}</p>
      </div>
    </div>
  )
}

export { LoadingOverlay }
