import { useAuthStore } from '@/stores/auth.store'
import { ROLE_LABELS } from '@/constants/roles'

/**
 * Placeholder dashboard page.
 * Will be replaced with role-specific dashboards in Phase 2.
 */
export default function DashboardPage() {
  const user = useAuthStore((s) => s.user)

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">Dashboard</h1>
        <p className="text-sm text-muted-foreground">
          Welcome back{user ? `, ${user.email}` : ''}.
          {user && (
            <span className="ml-1">
              Logged in as <strong>{ROLE_LABELS[user.role]}</strong>.
            </span>
          )}
        </p>
      </div>

      <div className="rounded-lg border border-dashed border-border p-8 text-center">
        <p className="text-sm text-muted-foreground">
          Phase 1 foundation is running correctly. Dashboard content arrives in Phase 2.
        </p>
      </div>
    </div>
  )
}
