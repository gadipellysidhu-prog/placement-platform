import { Outlet } from 'react-router-dom'

/** Bare-minimum layout — used for error pages and standalone screens. */
export function MinimalLayout() {
  return (
    <div className="min-h-screen bg-background">
      <Outlet />
    </div>
  )
}
