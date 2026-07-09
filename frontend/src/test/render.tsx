import { type ReactElement, type ReactNode } from 'react'
import { render, type RenderOptions, type RenderResult } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'
import { ThemeProvider } from '@/providers/ThemeProvider'
import { NotificationProvider } from '@/providers/NotificationProvider'
import { TooltipProvider } from '@/shared/ui/tooltip'

/**
 * Fresh QueryClient per render so cache never bleeds between tests. Retries are off
 * for deterministic assertions; gc/stale times are zeroed to avoid background timers.
 */
export function createTestQueryClient(): QueryClient {
  return new QueryClient({
    defaultOptions: {
      queries: { retry: false, gcTime: 0, staleTime: 0 },
      mutations: { retry: false },
    },
  })
}

interface RenderWithProvidersOptions extends Omit<RenderOptions, 'wrapper'> {
  /** Convenience for a single initial route (ignored if `initialEntries` is set). */
  route?: string
  /** Full MemoryRouter history stack. */
  initialEntries?: string[]
  /** Inject a pre-seeded client (e.g. to prime the cache); defaults to a fresh one. */
  queryClient?: QueryClient
}

export interface RenderWithProvidersResult extends RenderResult {
  queryClient: QueryClient
  user: ReturnType<typeof userEvent.setup>
}

/**
 * Renders `ui` inside the real application provider tree (Query → Theme → Tooltip →
 * Notifications → Router), mirroring App.tsx. BrowserRouter is swapped for MemoryRouter
 * so tests control navigation; every other provider is the production component.
 *
 * Future screens should only need `renderWithProviders(<Screen />, { route })`.
 */
export function renderWithProviders(
  ui: ReactElement,
  options: RenderWithProvidersOptions = {},
): RenderWithProvidersResult {
  const {
    route = '/',
    initialEntries = [route],
    queryClient = createTestQueryClient(),
    ...renderOptions
  } = options

  function Wrapper({ children }: { children: ReactNode }): ReactElement {
    return (
      <QueryClientProvider client={queryClient}>
        <ThemeProvider>
          <TooltipProvider>
            <NotificationProvider />
            <MemoryRouter initialEntries={initialEntries}>{children}</MemoryRouter>
          </TooltipProvider>
        </ThemeProvider>
      </QueryClientProvider>
    )
  }

  return {
    user: userEvent.setup(),
    queryClient,
    ...render(ui, { wrapper: Wrapper, ...renderOptions }),
  }
}
