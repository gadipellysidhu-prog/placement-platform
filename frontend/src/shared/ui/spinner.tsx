import { cva, type VariantProps } from 'class-variance-authority'
import { cn } from '@/utils/cn'

const spinnerVariants = cva(
  'animate-spin rounded-full border-2 border-current border-t-transparent',
  {
    variants: {
      size: {
        sm: 'h-4 w-4',
        md: 'h-6 w-6',
        lg: 'h-8 w-8',
        xl: 'h-12 w-12',
      },
    },
    defaultVariants: { size: 'md' },
  },
)

interface SpinnerProps extends VariantProps<typeof spinnerVariants> {
  className?: string
  label?: string
}

function Spinner({ size, className, label = 'Loading…' }: SpinnerProps) {
  return (
    <span role="status" className={cn('text-primary', className)}>
      <span className={cn(spinnerVariants({ size }))} aria-hidden="true" />
      <span className="sr-only">{label}</span>
    </span>
  )
}

export { Spinner }
