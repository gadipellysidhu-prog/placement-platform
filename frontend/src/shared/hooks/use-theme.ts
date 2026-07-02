import { useThemeStore } from '@/stores/theme.store'
import { useMediaQuery } from './use-media-query'

type Theme = 'light' | 'dark' | 'system'

interface UseThemeReturn {
  theme: Theme
  setTheme: (theme: Theme) => void
  isDark: boolean
}

/** Wraps the theme store and resolves system preference reactively via useMediaQuery. */
export function useTheme(): UseThemeReturn {
  const { theme, setTheme } = useThemeStore()
  const systemPrefersDark = useMediaQuery('(prefers-color-scheme: dark)')

  const isDark = theme === 'dark' || (theme === 'system' && systemPrefersDark)

  return { theme, setTheme, isDark }
}
