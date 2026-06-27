import { Spinner } from '@/shared/ui/spinner'

/** Full-screen loading spinner shown during lazy route chunk loading. */
export default function LoadingPage() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-background">
      <Spinner size="lg" />
    </div>
  )
}
