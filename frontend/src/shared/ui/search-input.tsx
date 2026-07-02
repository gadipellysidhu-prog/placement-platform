import * as React from 'react'
import { Search, X } from 'lucide-react'
import { Input, type InputProps } from './input'
import { cn } from '@/utils/cn'

interface SearchInputProps extends Omit<InputProps, 'type'> {
  onClear?: () => void
}

const SearchInput = React.forwardRef<HTMLInputElement, SearchInputProps>(
  ({ className, onClear, value, ...props }, ref) => {
    const hasValue = value !== undefined && value !== ''

    return (
      <div className={cn('relative flex items-center', className)}>
        <Search className="absolute left-3 h-4 w-4 text-muted-foreground" aria-hidden="true" />
        <Input
          ref={ref}
          type="search"
          value={value}
          className={cn('pl-9', hasValue && onClear ? 'pr-9' : '')}
          {...props}
        />
        {hasValue && onClear && (
          <button
            type="button"
            onClick={onClear}
            aria-label="Clear search"
            className="absolute right-3 text-muted-foreground hover:text-foreground focus-visible:outline-none"
          >
            <X className="h-4 w-4" />
          </button>
        )}
      </div>
    )
  },
)
SearchInput.displayName = 'SearchInput'

export { SearchInput }
