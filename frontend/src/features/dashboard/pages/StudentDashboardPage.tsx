import { Link } from 'react-router-dom'
import { FileCheck2, Gift, Award, UserCircle, ArrowRight } from 'lucide-react'
import { useAuthStore } from '@/stores/auth.store'
import { ROLE_LABELS } from '@/constants/roles'
import { ROUTES } from '@/constants/routes'
import { StatCard } from '@/shared/ui/stat-card'
import { StatusBadge } from '@/shared/ui/status-badge'
import { Button } from '@/shared/ui/button'
import { Card, CardContent } from '@/shared/ui/card'
import { useMyStudentProfile } from '@/features/students/hooks/use-students'
import { useQuery } from '@tanstack/react-query'
import { applicationsApi, offersApi, certificatesApi, queryKeys } from '@/lib/api'

function useStudentDashboardData() {
  const profile = useMyStudentProfile()

  const applications = useQuery({
    queryKey: queryKeys.applications.my(),
    queryFn: () => applicationsApi.my(),
    staleTime: 30_000,
  })

  const offers = useQuery({
    queryKey: queryKeys.offers.my(),
    queryFn: () => offersApi.my(),
    staleTime: 30_000,
  })

  const certificates = useQuery({
    queryKey: queryKeys.certificates.my(),
    queryFn: () => certificatesApi.my(),
    staleTime: 30_000,
  })

  return { profile, applications, offers, certificates }
}

export default function StudentDashboardPage() {
  const user = useAuthStore((s) => s.user)
  const { profile, applications, offers, certificates } = useStudentDashboardData()

  const isLoading =
    profile.isLoading || applications.isLoading || offers.isLoading || certificates.isLoading

  const pendingOffers = offers.data?.filter((o) => o.status === 'PENDING').length ?? 0

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

      {/* Profile not found CTA */}
      {!profile.isLoading && profile.error && (
        <Card>
          <CardContent className="flex items-center justify-between gap-4 p-6">
            <div className="flex items-center gap-3">
              <UserCircle className="h-8 w-8 text-muted-foreground shrink-0" aria-hidden="true" />
              <div>
                <p className="text-sm font-medium text-foreground">Student profile not set up</p>
                <p className="text-xs text-muted-foreground">
                  Contact your placement officer to create your profile.
                </p>
              </div>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Student profile summary */}
      {profile.data && (
        <Card>
          <CardContent className="flex items-center justify-between gap-4 p-6">
            <div className="space-y-1">
              <p className="text-sm font-medium text-foreground">
                {profile.data.rollNumber}
                {profile.data.branchName && (
                  <span className="ml-2 text-muted-foreground">— {profile.data.branchName}</span>
                )}
              </p>
              <div className="flex items-center gap-2">
                <StatusBadge status={profile.data.status} />
                {profile.data.placementEligible ? (
                  <span className="text-xs text-success font-medium">Eligible</span>
                ) : (
                  <span className="text-xs text-muted-foreground">Not eligible</span>
                )}
                {profile.data.cgpa != null && (
                  <span className="text-xs text-muted-foreground">CGPA: {profile.data.cgpa}</span>
                )}
              </div>
            </div>
            <Button variant="outline" size="sm" asChild>
              <Link to={ROUTES.STUDENT.PROFILE}>
                View profile <ArrowRight className="ml-1 h-3 w-3" />
              </Link>
            </Button>
          </CardContent>
        </Card>
      )}

      <div className="grid gap-4 sm:grid-cols-3">
        <StatCard
          title="Applications"
          value={applications.data?.length ?? '—'}
          icon={<FileCheck2 />}
          description="Total job applications"
          isLoading={isLoading}
        />
        <StatCard
          title="Pending Offers"
          value={pendingOffers}
          icon={<Gift />}
          description={`${offers.data?.length ?? 0} total offers`}
          isLoading={isLoading}
        />
        <StatCard
          title="Certificates"
          value={certificates.data?.length ?? '—'}
          icon={<Award />}
          description="Submitted for verification"
          isLoading={isLoading}
        />
      </div>
    </div>
  )
}
