import { Link } from 'react-router-dom'
import { ROUTES } from '@/constants/routes'

export default function NotFoundPage() {
  return (
    <main className="flex min-h-screen flex-col items-center justify-center gap-4 bg-background p-8 text-center">
      <p className="text-sm font-medium uppercase tracking-widest text-muted-foreground">
        404 — Page not found
      </p>
      <h1 className="text-4xl font-bold text-foreground">This page doesn&apos;t exist</h1>
      <p className="max-w-sm text-sm text-muted-foreground">
        The page you are looking for may have been moved, deleted, or never existed.
      </p>
      <Link
        to={ROUTES.DASHBOARD}
        className="mt-4 rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
      >
        Back to Dashboard
      </Link>
    </main>
  )
}
