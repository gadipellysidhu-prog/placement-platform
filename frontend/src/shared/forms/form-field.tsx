import * as React from 'react'
import { Label } from '@/shared/ui/label'
import { cn } from '@/utils/cn'

interface FormFieldProps {
  id?: string
  label?: string
  required?: boolean
  error?: string
  hint?: string
  className?: string
  children: React.ReactNode
}

/**
 * Wraps any input with a label, hint text, and error message.
 * Links the label to the input via htmlFor/id automatically.
 */
function FormField({ id, label, required, error, hint, className, children }: FormFieldProps) {
  const generatedId = React.useId()
  const inputId = id ?? generatedId
  const hintId = hint ? `${inputId}-hint` : undefined
  const errorId = error ? `${inputId}-error` : undefined

  const childWithProps = React.isValidElement<{
    id?: string
    'aria-describedby'?: string
    'aria-invalid'?: boolean
  }>(children)
    ? React.cloneElement(children, {
        id: children.props.id ?? inputId,
        'aria-describedby': [hintId, errorId].filter(Boolean).join(' ') || undefined,
        'aria-invalid': !!error,
      })
    : children

  return (
    <div className={cn('space-y-1.5', className)}>
      {label && (
        <Label htmlFor={inputId}>
          {label}
          {required && (
            <span className="ml-0.5 text-destructive" aria-hidden="true">
              *
            </span>
          )}
        </Label>
      )}
      {childWithProps}
      {hint && !error && (
        <p id={hintId} className="text-xs text-muted-foreground">
          {hint}
        </p>
      )}
      {error && (
        <p id={errorId} role="alert" className="text-xs text-destructive">
          {error}
        </p>
      )}
    </div>
  )
}

export { FormField }
