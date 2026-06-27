import { Toaster } from 'sonner'
import { useTheme } from '@/shared/hooks/use-theme'

/**
 * Renders the Sonner toaster with the correct theme.
 * Must be a child of ThemeProvider so useTheme works.
 */
export function NotificationProvider() {
  const { isDark } = useTheme()

  return (
    <Toaster
      theme={isDark ? 'dark' : 'light'}
      position="top-right"
      richColors
      closeButton
      duration={4000}
      toastOptions={{
        classNames: {
          toast: 'font-sans text-sm',
        },
      }}
    />
  )
}
