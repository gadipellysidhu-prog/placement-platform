import { Outlet } from 'react-router-dom'

/**
 * Centered card layout for public auth pages (login, register, forgot-password).
 * Responsive: full-bleed on mobile, max-w-md card on larger screens.
 */
export function AuthLayout() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center bg-background px-4 py-12">
      {/* Brand */}
      <div className="mb-8 text-center">
        <h1 className="text-2xl font-bold tracking-tight text-foreground">
          Placement Intelligence
        </h1>
        <p className="mt-1 text-sm text-muted-foreground">Skill Verification Platform</p>
      </div>

      {/* Card */}
      <div className="w-full max-w-md rounded-xl border border-border bg-card p-8 shadow-sm">
        <Outlet />
      </div>
    </div>
  )
}
