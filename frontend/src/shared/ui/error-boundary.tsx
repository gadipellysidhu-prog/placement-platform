import { Component, type ErrorInfo, type ReactNode } from 'react'
import { AlertTriangle, RefreshCw } from 'lucide-react'

interface Props {
  children: ReactNode
  fallback?: ReactNode
  /** Called after an error is caught — use for logging. */
  onError?: (error: Error, info: ErrorInfo) => void
  /** If true, renders a minimal inline error rather than a full-page fallback. */
  inline?: boolean
}

interface State {
  hasError: boolean
  error: Error | null
}

/**
 * Class-based error boundary (hooks cannot replace componentDidCatch).
 * Catches rendering exceptions from all descendant components.
 *
 * Three usage modes:
 * 1. Global  — wraps the entire app, shows full-page recovery UI
 * 2. Feature — wraps a feature section, shows inline error with retry
 * 3. Custom  — caller provides `fallback` prop for full control
 */
export class ErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props)
    this.state = { hasError: false, error: null }
  }

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error }
  }

  componentDidCatch(error: Error, info: ErrorInfo): void {
    console.error('[ErrorBoundary] Uncaught error:', error, info.componentStack)
    this.props.onError?.(error, info)
  }

  private reset = () => {
    this.setState({ hasError: false, error: null })
  }

  render(): ReactNode {
    if (!this.state.hasError) return this.props.children

    if (this.props.fallback) return this.props.fallback

    if (this.props.inline) {
      return (
        <div
          role="alert"
          className="flex items-center gap-3 rounded-lg border border-destructive/50 bg-destructive/10 p-4 text-sm text-destructive"
        >
          <AlertTriangle className="h-4 w-4 shrink-0" aria-hidden="true" />
          <span className="flex-1">
            {this.state.error?.message ?? 'An unexpected error occurred.'}
          </span>
          <button
            onClick={this.reset}
            className="ml-auto flex items-center gap-1 text-xs underline-offset-2 hover:underline focus-visible:outline-none"
            aria-label="Retry"
          >
            <RefreshCw className="h-3 w-3" />
            Retry
          </button>
        </div>
      )
    }

    return (
      <div
        role="alert"
        className="flex min-h-[400px] flex-col items-center justify-center gap-4 p-8 text-center"
      >
        <AlertTriangle className="h-12 w-12 text-destructive" aria-hidden="true" />
        <h2 className="text-xl font-semibold text-foreground">Something went wrong</h2>
        <p className="max-w-sm text-sm text-muted-foreground">
          An unexpected error occurred. Reload the page to continue. If the problem persists,
          contact support.
        </p>
        <div className="flex gap-3">
          <button
            onClick={this.reset}
            className="rounded-md border border-border px-4 py-2 text-sm font-medium text-foreground hover:bg-accent focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
          >
            Try again
          </button>
          <button
            onClick={() => window.location.reload()}
            className="rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
          >
            Reload page
          </button>
        </div>
      </div>
    )
  }
}
