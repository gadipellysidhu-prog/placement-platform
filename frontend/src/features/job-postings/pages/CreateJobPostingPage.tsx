import { useNavigate } from 'react-router-dom'
import { toast } from 'sonner'
import { ChevronLeft } from 'lucide-react'
import { PageHeader } from '@/shared/ui/page-header'
import { Button } from '@/shared/ui/button'
import { Card, CardContent } from '@/shared/ui/card'
import { ROUTES } from '@/constants/routes'
import { getApiErrorMessage } from '@/lib/api'
import { JobPostingForm } from '../components/JobPostingForm'
import { toCreatePayload, type JobPostingFormValues } from '../job-posting.schema'
import { useCreateJobPosting } from '../hooks/use-job-postings'

export default function CreateJobPostingPage() {
  const navigate = useNavigate()
  const { mutateAsync: createJobPosting, isPending } = useCreateJobPosting()

  async function onSubmit(values: JobPostingFormValues) {
    try {
      const posting = await createJobPosting(toCreatePayload(values))
      toast.success(`“${posting.title}” created as a draft`)
      navigate(ROUTES.OFFICER.JOB_POSTING_DETAIL(posting.id))
    } catch (err) {
      toast.error(getApiErrorMessage(err))
    }
  }

  return (
    <div className="space-y-6">
      <Button variant="ghost" size="sm" onClick={() => navigate(ROUTES.OFFICER.JOB_POSTINGS)}>
        <ChevronLeft className="h-4 w-4" /> Back to job postings
      </Button>

      <PageHeader
        title="Create Job Posting"
        description="New postings start as a draft. Add skills and branches, then open it for applications."
      />

      <Card className="max-w-2xl">
        <CardContent className="pt-6">
          <JobPostingForm
            mode="create"
            isSubmitting={isPending}
            submitLabel="Create draft"
            onSubmit={onSubmit}
            onCancel={() => navigate(ROUTES.OFFICER.JOB_POSTINGS)}
          />
        </CardContent>
      </Card>
    </div>
  )
}
