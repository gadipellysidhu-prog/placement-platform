import { lazy, Suspense } from 'react'
import { useAuthStore } from '@/stores/auth.store'
import { ROLES } from '@/constants/roles'
import { Spinner } from '@/shared/ui/spinner'

const StudentDashboardPage = lazy(() => import('@/features/dashboard/pages/StudentDashboardPage'))
const OfficerDashboardPage = lazy(() => import('@/features/dashboard/pages/OfficerDashboardPage'))

export default function DashboardPage() {
  const role = useAuthStore((s) => s.user?.role)

  return (
    <Suspense
      fallback={
        <div className="flex h-48 items-center justify-center">
          <Spinner />
        </div>
      }
    >
      {role === ROLES.STUDENT ? <StudentDashboardPage /> : <OfficerDashboardPage />}
    </Suspense>
  )
}
