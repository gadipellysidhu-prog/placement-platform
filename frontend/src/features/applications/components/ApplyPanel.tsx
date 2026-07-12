import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { toast } from 'sonner'
import { AlertCircle, CheckCircle2 } from 'lucide-react'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/ui/card'
import { Button } from '@/shared/ui/button'
import { Alert, AlertDescription, AlertTitle } from '@/shared/ui/alert'
import { StatusBadge } from '@/shared/ui/status-badge'
import { ROUTES } from '@/constants/routes'
import { getApiErrorMessage } from '@/lib/api'
import type { JobPostingStatus } from '@/lib/api'
import { useMyStudentProfile } from '@/features/students/hooks/use-students'
import { useApplyToJob, useMyApplications } from '../hooks/use-applications'

interface ApplyPanelProps {
  postingId: string
  postingStatus: JobPostingStatus
}

/**
 * Student-only application panel rendered inside the job posting detail page.
 * Mounted exclusively for students, so an officer never triggers the `/students/me`
 * or `/applications/my` fetches.
 *
 * Eligibility, "posting not open" and "already applied" are all enforced by the
 * backend; this panel mirrors those rules to disable the button up front and renders
 * the backend's RFC 7807 ProblemDetail reasons verbatim when a submission is rejected.
 */
export function ApplyPanel({ postingId, postingStatus }: ApplyPanelProps) {
  const navigate = useNavigate()
  const profile = useMyStudentProfile()
  const myApplications = useMyApplications()
  const apply = useApplyToJob()
  const [errorDetail, setErrorDetail] = useState<string | null>(null)

  // Backend `apply` rejects any posting the student has a row for (existsByStudentAndJobPosting),
  // regardless of status — so a withdrawn application still blocks re-applying.
  const existing = myApplications.data?.find((a) => a.jobPostingId === postingId)
  const alreadyApplied = Boolean(existing)
  const postingOpen = postingStatus === 'OPEN'
  const loading = profile.isLoading || myApplications.isLoading
  const missingProfile = !profile.isLoading && !profile.data

  const disabled = apply.isPending || loading || !postingOpen || alreadyApplied || !profile.data

  async function handleApply() {
    if (!profile.data) return
    setErrorDetail(null)
    try {
      await apply.mutateAsync({ studentId: profile.data.id, jobPostingId: postingId })
      toast.success('Application submitted')
      navigate(ROUTES.STUDENT.MY_APPLICATIONS)
    } catch (err) {
      // Surfaces every backend ProblemDetail reason exactly (422 eligibility detail,
      // 409 duplicate, 422 "posting not open", etc.).
      const message = getApiErrorMessage(err)
      setErrorDetail(message)
      toast.error(message)
    }
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Apply</CardTitle>
      </CardHeader>
      <CardContent className="space-y-3">
        {alreadyApplied && existing ? (
          <Alert variant="success">
            <CheckCircle2 className="h-4 w-4" aria-hidden="true" />
            <AlertTitle>You’ve already applied</AlertTitle>
            <AlertDescription className="space-y-2">
              <div className="flex items-center gap-2">
                <span>Current status:</span>
                <StatusBadge status={existing.status} />
              </div>
              <Link
                to={ROUTES.STUDENT.MY_APPLICATIONS}
                className="text-sm font-medium text-primary underline-offset-4 hover:underline"
              >
                View my applications
              </Link>
            </AlertDescription>
          </Alert>
        ) : (
          <>
            <Button className="w-full" disabled={disabled} onClick={handleApply}>
              {apply.isPending ? 'Submitting…' : 'Apply now'}
            </Button>

            {!postingOpen && (
              <p className="text-xs text-muted-foreground">
                This posting is not open for applications.
              </p>
            )}

            {missingProfile && (
              <p className="text-xs text-muted-foreground">
                Your student profile isn’t set up yet — contact your placement officer.
              </p>
            )}

            {errorDetail && (
              <Alert variant="destructive">
                <AlertCircle className="h-4 w-4" aria-hidden="true" />
                <AlertTitle>Unable to apply</AlertTitle>
                <AlertDescription>{errorDetail}</AlertDescription>
              </Alert>
            )}
          </>
        )}
      </CardContent>
    </Card>
  )
}
