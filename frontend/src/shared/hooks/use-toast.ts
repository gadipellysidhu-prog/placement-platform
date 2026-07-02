import { toast } from 'sonner'

interface UseToastReturn {
  success: (message: string, description?: string) => void
  error: (message: string, description?: string) => void
  warning: (message: string, description?: string) => void
  info: (message: string, description?: string) => void
  loading: (message: string) => string | number
  dismiss: (id?: string | number) => void
  promise: <T>(
    promise: Promise<T>,
    options: { loading: string; success: string; error: string },
  ) => void
}

/** Thin wrapper over sonner for consistent toast API across the app. */
export function useToast(): UseToastReturn {
  return {
    success: (message, description) => toast.success(message, { description }),
    error: (message, description) => toast.error(message, { description }),
    warning: (message, description) => toast.warning(message, { description }),
    info: (message, description) => toast.info(message, { description }),
    loading: (message) => toast.loading(message),
    dismiss: (id) => toast.dismiss(id),
    promise: (promise, options) => {
      toast.promise(promise, {
        loading: options.loading,
        success: options.success,
        error: options.error,
      })
    },
  }
}
