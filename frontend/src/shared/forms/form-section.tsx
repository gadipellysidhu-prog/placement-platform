import { cn } from '@/utils/cn'

interface FormSectionProps {
  title?: string
  description?: string
  className?: string
  children: React.ReactNode
}

function FormSection({ title, description, className, children }: FormSectionProps) {
  return (
    <fieldset className={cn('space-y-4', className)}>
      {(title || description) && (
        <div className="space-y-1">
          {title && <legend className="text-sm font-semibold text-foreground">{title}</legend>}
          {description && <p className="text-xs text-muted-foreground">{description}</p>}
        </div>
      )}
      {children}
    </fieldset>
  )
}

export { FormSection }
