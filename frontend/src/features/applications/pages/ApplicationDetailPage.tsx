import { type ReactNode } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { ChevronLeft } from 'lucide-react'
import { PageHeader } from '@/shared/ui/page-header'
import { StatusBadge } from '@/shared/ui/status-badge'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/ui/card'
import { Button } from '@/shared/ui/button'
import { Skeleton } from '@/shared/ui/skeleton'
import { ErrorState } from '@/shared/ui/error-state'
import { EmptyState } from '@/shared/ui/empty-state'
import { ROUTES } from '@/constants/routes'
import { usePermission } from '@/shared/hooks/use-permission'
import { formatDateTime } from '@/utils/format'
import type { JobApplicationResponse } from '@/lib/api'
import { ApplicationTimeline } from '../components/ApplicationTimeline'
import { StatusTransition } from '../components/StatusTransition'
import { WithdrawAction } from '../components/WithdrawAction'
import { canWithdraw } from '../applications.transitions'
import { useApplication, useMyApplications } from '../hooks/use-applications'

/**
 * Role-aware application detail.
 *
 * The backend `GET /api/applications/{id}` endpoint is officer-only, so officers load
 * the application directly while students derive it from their own `/my` list. A student
 * who does not own the application simply won't find it there — yielding a 403-safe
 * "no access" view with no data leak and no failed request.
 */
export default function ApplicationDetailPage() {
  const { id } = useParams<{ id: string }>()
  const { isOfficer } = usePermission()

  return isOfficer ? <OfficerApplicationDetail id={id!} /> : <StudentApplicationDetail id={id!} />
}

function OfficerApplicationDetail({ id }: { id: string }) {
  const navigate = useNavigate()
  const { data, isLoading, isError, refetch } = useApplication(id)

  if (isLoading) return <DetailSkeleton />
  if (isError || !data) {
    return (
      <BackShell onBack={() => navigate(ROUTES.OFFICER.APPLICATIONS)}>
        <ErrorState title="Application not found" onRetry={() => refetch()} />
      </BackShell>
    )
  }

  return (
    <ApplicationDetailView
      application={data}
      onBack={() => navigate(ROUTES.OFFICER.APPLICATIONS)}
      actions={
        <>
          <p className="text-sm text-muted-foreground">Move this application to its next stage.</p>
          <StatusTransition application={data} />
        </>
      }
    />
  )
}

function StudentApplicationDetail({ id }: { id: string }) {
  const navigate = useNavigate()
  const { data, isLoading, isError, refetch } = useMyApplications()

  if (isLoading) return <DetailSkeleton />
  if (isError) {
    return (
      <BackShell onBack={() => navigate(ROUTES.STUDENT.MY_APPLICATIONS)}>
        <ErrorState title="Couldn’t load your applications" onRetry={() => refetch()} />
      </BackShell>
    )
  }

  const application = data?.find((a) => a.id === id)
  if (!application) {
    // 403-safe: a student can only ever see their own applications.
    return (
      <BackShell onBack={() => navigate(ROUTES.STUDENT.MY_APPLICATIONS)}>
        <EmptyState
          title="Application not available"
          description="This application doesn’t exist or you don’t have access to it."
          action={
            <Button
              variant="outline"
              size="sm"
              onClick={() => navigate(ROUTES.STUDENT.MY_APPLICATIONS)}
            >
              Back to my applications
            </Button>
          }
        />
      </BackShell>
    )
  }

  return (
    <ApplicationDetailView
      application={application}
      onBack={() => navigate(ROUTES.STUDENT.MY_APPLICATIONS)}
      actions={
        canWithdraw(application.status) ? (
          <>
            <p className="text-sm text-muted-foreground">
              You can withdraw this application while it is still active.
            </p>
            <WithdrawAction application={application} fullWidth />
          </>
        ) : (
          <p className="text-sm text-muted-foreground">
            This application can no longer be withdrawn.
          </p>
        )
      }
    />
  )
}

/** Shared presentational layout — reused verbatim by both role views. */
function ApplicationDetailView({
  application,
  actions,
  onBack,
}: {
  application: JobApplicationResponse
  actions: ReactNode
  onBack: () => void
}) {
  return (
    <div className="space-y-6">
      <Button variant="ghost" size="sm" onClick={onBack}>
        <ChevronLeft className="h-4 w-4" /> Back
      </Button>

      <PageHeader title={application.jobPostingTitle} description={application.companyName} />

      <div className="grid gap-4 lg:grid-cols-3">
        <div className="space-y-4 lg:col-span-2">
          <Card>
            <CardHeader>
              <CardTitle>Summary</CardTitle>
            </CardHeader>
            <CardContent className="space-y-3">
              <Field label="Student" value={application.studentRollNumber} />
              <Field label="Posting" value={application.jobPostingTitle} />
              <Field label="Company" value={application.companyName} />
              <div className="flex items-center justify-between gap-4">
                <span className="text-sm text-muted-foreground shrink-0">Status</span>
                <StatusBadge status={application.status} />
              </div>
              <Field label="Applied" value={formatDateTime(application.appliedAt)} />
              <Field label="Last updated" value={formatDateTime(application.updatedAt)} />
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Timeline</CardTitle>
            </CardHeader>
            <CardContent>
              <ApplicationTimeline application={application} />
            </CardContent>
          </Card>
        </div>

        <div className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle>Actions</CardTitle>
            </CardHeader>
            <CardContent className="space-y-3">{actions}</CardContent>
          </Card>
        </div>
      </div>
    </div>
  )
}

function BackShell({ children, onBack }: { children: ReactNode; onBack: () => void }) {
  return (
    <div className="space-y-6">
      <Button variant="ghost" size="sm" onClick={onBack}>
        <ChevronLeft className="h-4 w-4" /> Back
      </Button>
      {children}
    </div>
  )
}

function DetailSkeleton() {
  return (
    <div className="space-y-6">
      <Skeleton className="h-9 w-72" />
      <div className="grid gap-4 lg:grid-cols-3">
        <Skeleton className="h-64 rounded-xl lg:col-span-2" />
        <Skeleton className="h-64 rounded-xl" />
      </div>
    </div>
  )
}

function Field({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-center justify-between gap-4">
      <span className="text-sm text-muted-foreground shrink-0">{label}</span>
      <span className="text-sm font-medium text-foreground text-right">{value}</span>
    </div>
  )
}
