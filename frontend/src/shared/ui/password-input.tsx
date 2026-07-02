import * as React from 'react'
import { Eye, EyeOff } from 'lucide-react'
import { Input, type InputProps } from './input'

const PasswordInput = React.forwardRef<HTMLInputElement, Omit<InputProps, 'type'>>(
  ({ ...props }, ref) => {
    const [visible, setVisible] = React.useState(false)

    return (
      <Input
        ref={ref}
        type={visible ? 'text' : 'password'}
        endAdornment={
          <button
            type="button"
            tabIndex={-1}
            aria-label={visible ? 'Hide password' : 'Show password'}
            onClick={() => setVisible((v) => !v)}
            className="text-muted-foreground hover:text-foreground focus-visible:outline-none"
          >
            {visible ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
          </button>
        }
        {...props}
      />
    )
  },
)
PasswordInput.displayName = 'PasswordInput'

export { PasswordInput }
