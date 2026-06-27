import { useEffect, type ReactNode } from 'react'
import { useThemeStore } from '@/stores/theme.store'

interface ThemeProviderProps {
  children: ReactNode
}

/**
 * Applies the `dark` class to <html> based on the persisted theme preference.
 * Reads `prefers-color-scheme` for the "system" setting.
 */
export function ThemeProvider({ children }: ThemeProviderProps) {
  const theme = useThemeStore((s) => s.theme)

  useEffect(() => {
    const root = document.documentElement
    const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches

    if (theme === 'system') {
      root.classList.toggle('dark', prefersDark)
    } else {
      root.classList.toggle('dark', theme === 'dark')
    }
  }, [theme])

  return <>{children}</>
}
