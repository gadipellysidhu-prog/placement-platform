import { UserCircle } from 'lucide-react'
import { PageHeader } from '@/shared/ui/page-header'
import { StatusBadge } from '@/shared/ui/status-badge'
import { ErrorState } from '@/shared/ui/error-state'
import { Skeleton } from '@/shared/ui/skeleton'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/ui/card'
import { Badge } from '@/shared/ui/badge'
import { useMyStudentProfile } from '../hooks/use-students'

export default function StudentProfilePage() {
  const { data: student, isLoading, isError, refetch } = useMyStudentProfile()

  if (isLoading) {
    return (
      <div className="space-y-6">
        <Skeleton className="h-9 w-48" />
        <div className="grid gap-4 md:grid-cols-2">
          <Skeleton className="h-40 rounded-xl" />
          <Skeleton className="h-40 rounded-xl" />
        </div>
      </div>
    )
  }

  if (isError || !student) {
    return (
      <div className="space-y-6">
        <PageHeader title="My Profile" />
        <ErrorState
          title="Profile not found"
          description="Your student profile has not been created yet. Contact your placement officer."
          onRetry={() => refetch()}
        />
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <PageHeader title="My Profile" description="Your student placement profile" />

      <div className="grid gap-4 md:grid-cols-2">
        <Card>
          <CardHeader>
            <div className="flex items-center gap-3">
              <div
                aria-hidden="true"
                className="flex h-10 w-10 items-center justify-center rounded-full bg-primary/10"
              >
                <UserCircle className="h-5 w-5 text-primary" />
              </div>
              <CardTitle>Personal Information</CardTitle>
            </div>
          </CardHeader>
          <CardContent className="space-y-3">
            <Field label="Email" value={student.userEmail} />
            <Field label="Roll Number" value={student.rollNumber} />
            <Field label="Branch" value={student.branchName ?? '—'} />
            <Field label="Current Year" value={`Year ${student.currentYear}`} />
            <Field label="CGPA" value={student.cgpa != null ? String(student.cgpa) : '—'} />
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Placement Status</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="space-y-1">
              <span className="text-xs font-medium text-muted-foreground uppercase tracking-wide">
                Status
              </span>
              <div className="flex items-center gap-2">
                <StatusBadge status={student.status} />
              </div>
            </div>
            <div className="space-y-1">
              <span className="text-xs font-medium text-muted-foreground uppercase tracking-wide">
                Placement Eligibility
              </span>
              <p className="text-sm font-medium">
                {student.placementEligible ? (
                  <span className="text-green-600 dark:text-green-400">Eligible</span>
                ) : (
                  <span className="text-muted-foreground">Not eligible</span>
                )}
              </p>
            </div>
            {student.skillNames.length > 0 && (
              <div className="space-y-2">
                <span className="text-xs font-medium text-muted-foreground uppercase tracking-wide">
                  Skills
                </span>
                <div className="flex flex-wrap gap-1.5">
                  {student.skillNames.map((skill) => (
                    <Badge key={skill} variant="secondary">
                      {skill}
                    </Badge>
                  ))}
                </div>
              </div>
            )}
          </CardContent>
        </Card>
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
