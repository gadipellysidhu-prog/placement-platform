import { useNavigate, useParams } from 'react-router-dom'
import { toast } from 'sonner'
import { ChevronLeft } from 'lucide-react'
import { PageHeader } from '@/shared/ui/page-header'
import { Button } from '@/shared/ui/button'
import { Card, CardContent } from '@/shared/ui/card'
import { ErrorState } from '@/shared/ui/error-state'
import { Skeleton } from '@/shared/ui/skeleton'
import { Alert, AlertDescription, AlertTitle } from '@/shared/ui/alert'
import { ROUTES } from '@/constants/routes'
import { getApiErrorMessage } from '@/lib/api'
import { JobPostingForm, type JobPostingFormDefaults } from '../components/JobPostingForm'
import { toUpdatePayload, type JobPostingFormValues } from '../job-posting.schema'
import { useJobPosting, useUpdateJobPosting } from '../hooks/use-job-postings'

/** Render a nullable number as a controlled-input string ('' when absent). */
function numToInput(value: number | null): string {
  return value == null ? '' : String(value)
}

export default function EditJobPostingPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()

  const { data: posting, isLoading, isError, refetch } = useJobPosting(id!)
  const { mutateAsync: updateJobPosting, isPending } = useUpdateJobPosting(id!)

  const backToDetail = () => navigate(ROUTES.OFFICER.JOB_POSTING_DETAIL(id!))

  async function onSubmit(values: JobPostingFormValues) {
    try {
      await updateJobPosting(toUpdatePayload(values))
      toast.success('Job posting updated')
      backToDetail()
    } catch (err) {
      toast.error(getApiErrorMessage(err))
    }
  }

  if (isLoading) {
    return (
      <div className="space-y-6">
        <Skeleton className="h-9 w-64" />
        <Skeleton className="h-96 max-w-2xl rounded-xl" />
      </div>
    )
  }

  if (isError || !posting) {
    return (
      <div className="space-y-6">
        <Button variant="ghost" size="sm" onClick={() => navigate(ROUTES.OFFICER.JOB_POSTINGS)}>
          <ChevronLeft className="h-4 w-4" /> Back to job postings
        </Button>
        <ErrorState title="Job posting not found" onRetry={() => refetch()} />
      </div>
    )
  }

  const isDraft = posting.status === 'DRAFT'

  const defaults: JobPostingFormDefaults = {
    companyId: posting.companyId,
    title: posting.title,
    description: posting.description ?? '',
    ctcMin: numToInput(posting.ctcMin),
    ctcMax: numToInput(posting.ctcMax),
    applicationDeadline: posting.applicationDeadline ?? '',
    offerLimit: String(posting.offerLimit),
  }

  return (
    <div className="space-y-6">
      <Button variant="ghost" size="sm" onClick={backToDetail}>
        <ChevronLeft className="h-4 w-4" /> Back to posting
      </Button>

      <PageHeader title="Edit Job Posting" description={posting.title} />

      {!isDraft ? (
        <Alert className="max-w-2xl">
          <AlertTitle>This posting can no longer be edited</AlertTitle>
          <AlertDescription>
            Only draft postings can be edited. This posting is {posting.status.toLowerCase()}.
          </AlertDescription>
        </Alert>
      ) : (
        <Card className="max-w-2xl">
          <CardContent className="pt-6">
            <JobPostingForm
              mode="edit"
              defaultValues={defaults}
              companyName={posting.companyName}
              isSubmitting={isPending}
              submitLabel="Save changes"
              onSubmit={onSubmit}
              onCancel={backToDetail}
            />
          </CardContent>
        </Card>
      )}
    </div>
  )
}
