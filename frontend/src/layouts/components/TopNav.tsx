import { Menu } from 'lucide-react'
import { useAuthStore } from '@/stores/auth.store'
import { useUIStore } from '@/stores/ui.store'
import { ROLE_LABELS } from '@/constants/roles'

/** Top navigation bar — hamburger (mobile) + user info chip. */
export function TopNav() {
  const user = useAuthStore((s) => s.user)
  const toggleSidebar = useUIStore((s) => s.toggleSidebar)

  return (
    <header className="flex h-14 shrink-0 items-center justify-between border-b border-border bg-card px-4">
      {/* Mobile hamburger */}
      <button
        onClick={toggleSidebar}
        className="rounded-md p-1.5 text-muted-foreground hover:bg-accent hover:text-foreground lg:hidden"
        aria-label="Open navigation menu"
      >
        <Menu className="h-5 w-5" aria-hidden="true" />
      </button>

      {/* Desktop spacer */}
      <div className="hidden lg:block" aria-hidden="true" />

      {/* User info */}
      {user && (
        <div className="flex items-center gap-3">
          <div className="hidden text-right sm:block">
            <p className="text-sm font-medium leading-none text-foreground">{user.email}</p>
            <p className="mt-0.5 text-xs text-muted-foreground">{ROLE_LABELS[user.role]}</p>
          </div>
          <div
            aria-hidden="true"
            className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-primary text-xs font-semibold uppercase text-primary-foreground"
          >
            {user.email.charAt(0)}
          </div>
        </div>
      )}
    </header>
  )
}
