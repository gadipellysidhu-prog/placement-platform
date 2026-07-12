import { Link } from 'react-router-dom'
import { FileCheck2, ArrowRight, Briefcase } from 'lucide-react'
import { PageHeader } from '@/shared/ui/page-header'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/ui/card'
import { StatusBadge } from '@/shared/ui/status-badge'
import { Skeleton } from '@/shared/ui/skeleton'
import { ErrorState } from '@/shared/ui/error-state'
import { EmptyState } from '@/shared/ui/empty-state'
import { Button } from '@/shared/ui/button'
import { ROUTES } from '@/constants/routes'
import { ApplicationTimeline } from '../components/ApplicationTimeline'
import { WithdrawAction } from '../components/WithdrawAction'
import { useMyApplications } from '../hooks/use-applications'

export default function MyApplicationsPage() {
  const { data, isLoading, isError, refetch } = useMyApplications()

  return (
    <div className="space-y-6">
      <PageHeader
        title="My Applications"
        description="Track every job you’ve applied to and its current status."
        action={
          <Button asChild variant="outline">
            <Link to={ROUTES.STUDENT.JOB_POSTINGS}>
              <Briefcase className="h-4 w-4" /> Browse jobs
            </Link>
          </Button>
        }
      />

      {isLoading && (
        <div className="grid gap-4">
          <Skeleton className="h-40 rounded-xl" />
          <Skeleton className="h-40 rounded-xl" />
        </div>
      )}

      {isError && <ErrorState title="Couldn’t load your applications" onRetry={() => refetch()} />}

      {!isLoading && !isError && data && data.length === 0 && (
        <EmptyState
          icon={<FileCheck2 />}
          title="No applications yet"
          description="Browse open postings and apply to get started."
          action={
            <Button asChild variant="outline" size="sm">
              <Link to={ROUTES.STUDENT.JOB_POSTINGS}>
                <Briefcase className="h-4 w-4" /> Browse jobs
              </Link>
            </Button>
          }
        />
      )}

      {!isLoading && !isError && data && data.length > 0 && (
        <ul className="grid gap-4">
          {data.map((application) => (
            <li key={application.id}>
              <Card>
                <CardHeader className="flex flex-row items-start justify-between gap-4 space-y-0">
                  <div className="min-w-0 space-y-1">
                    <CardTitle className="truncate">
                      <Link
                        to={ROUTES.STUDENT.MY_APPLICATION_DETAIL(application.id)}
                        className="hover:underline underline-offset-4"
                      >
                        {application.jobPostingTitle}
                      </Link>
                    </CardTitle>
                    <p className="text-sm text-muted-foreground truncate">
                      {application.companyName}
                    </p>
                  </div>
                  <div className="flex shrink-0 items-center gap-2">
                    <StatusBadge status={application.status} />
                  </div>
                </CardHeader>
                <CardContent className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
                  <ApplicationTimeline application={application} />
                  <div className="flex items-center gap-2">
                    <Button asChild variant="ghost" size="sm">
                      <Link to={ROUTES.STUDENT.JOB_POSTING_DETAIL(application.jobPostingId)}>
                        View posting <ArrowRight className="ml-1 h-3 w-3" />
                      </Link>
                    </Button>
                    <WithdrawAction application={application} />
                  </div>
                </CardContent>
              </Card>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
