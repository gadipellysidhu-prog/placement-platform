import {
  Users,
  Building2,
  Briefcase,
  FileCheck2,
  Award,
  TrendingUp,
  GraduationCap,
} from 'lucide-react'
import { useAuthStore } from '@/stores/auth.store'
import { ROLE_LABELS } from '@/constants/roles'
import { StatCard } from '@/shared/ui/stat-card'
import { useDashboardSummary } from '../hooks/use-dashboard-summary'

export default function OfficerDashboardPage() {
  const user = useAuthStore((s) => s.user)
  const { data, isLoading } = useDashboardSummary()

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

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard
          title="Total Students"
          value={data?.totalStudents ?? '—'}
          icon={<Users />}
          isLoading={isLoading}
        />
        <StatCard
          title="Placed Students"
          value={data?.placedStudents ?? '—'}
          icon={<GraduationCap />}
          isLoading={isLoading}
        />
        <StatCard
          title="Active Companies"
          value={data?.activeCompanies ?? '—'}
          icon={<Building2 />}
          isLoading={isLoading}
        />
        <StatCard
          title="Open Job Postings"
          value={data?.openJobPostings ?? '—'}
          icon={<Briefcase />}
          isLoading={isLoading}
        />
        <StatCard
          title="Pending Applications"
          value={data?.pendingApplications ?? '—'}
          icon={<FileCheck2 />}
          isLoading={isLoading}
        />
        <StatCard
          title="Pending Certificates"
          value={data?.pendingCertificates ?? '—'}
          icon={<Award />}
          isLoading={isLoading}
        />
        <StatCard
          title="Placement Rate"
          value={data != null ? `${data.placementRatePercent.toFixed(1)}%` : '—'}
          icon={<TrendingUp />}
          description={
            data ? `${data.placedStudents} of ${data.totalStudents} students` : undefined
          }
          isLoading={isLoading}
        />
      </div>
    </div>
  )
}
