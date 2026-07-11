import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { ChevronLeft, Pencil } from 'lucide-react'
import { toast } from 'sonner'
import { PageHeader } from '@/shared/ui/page-header'
import { StatusBadge } from '@/shared/ui/status-badge'
import { ErrorState } from '@/shared/ui/error-state'
import { Skeleton } from '@/shared/ui/skeleton'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/ui/card'
import { Button } from '@/shared/ui/button'
import { Badge } from '@/shared/ui/badge'
import { ROUTES } from '@/constants/routes'
import { ROLES } from '@/constants/roles'
import { getApiErrorMessage } from '@/lib/api'
import { useAuthStore } from '@/stores/auth.store'
import { formatCTCRange, formatDate, formatDateTime } from '@/utils/format'
import { ConfirmDialog } from '../components/ConfirmDialog'
import { TagSection } from '../components/TagSection'
import {
  useAddJobPostingBranch,
  useAddJobPostingSkill,
  useCancelJobPosting,
  useCloseJobPosting,
  useJobPosting,
  useOpenJobPosting,
  useRemoveJobPostingBranch,
  useRemoveJobPostingSkill,
} from '../hooks/use-job-postings'
import { useBranches, useSkills } from '../hooks/use-lookups'

type LifecycleAction = 'open' | 'close' | 'cancel'

const LIFECYCLE_COPY: Record<
  LifecycleAction,
  { title: string; description: string; confirmLabel: string; destructive?: boolean }
> = {
  open: {
    title: 'Open this posting?',
    description: 'Students will be able to see and apply to this posting once it is open.',
    confirmLabel: 'Open posting',
  },
  close: {
    title: 'Close this posting?',
    description: 'Closing stops new applications. This cannot be reopened.',
    confirmLabel: 'Close posting',
  },
  cancel: {
    title: 'Cancel this posting?',
    description: 'Cancelling permanently withdraws this posting. This cannot be undone.',
    confirmLabel: 'Cancel posting',
    destructive: true,
  },
}

export default function JobPostingDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const user = useAuthStore((s) => s.user)
  const isOfficer = user?.role === ROLES.PLACEMENT_OFFICER || user?.role === ROLES.ADMIN

  const [dialogAction, setDialogAction] = useState<LifecycleAction | null>(null)

  const { data: posting, isLoading, isError, refetch } = useJobPosting(id!)

  const openPosting = useOpenJobPosting()
  const closePosting = useCloseJobPosting()
  const cancelPosting = useCancelJobPosting()

  const addSkill = useAddJobPostingSkill(id!)
  const removeSkill = useRemoveJobPostingSkill(id!)
  const addBranch = useAddJobPostingBranch(id!)
  const removeBranch = useRemoveJobPostingBranch(id!)

  // Lookup options are only needed by the officer tagging UI.
  const skillsQuery = useSkills()
  const branchesQuery = useBranches()

  const listRoute = isOfficer ? ROUTES.OFFICER.JOB_POSTINGS : ROUTES.STUDENT.JOB_POSTINGS

  const lifecyclePending =
    openPosting.isPending || closePosting.isPending || cancelPosting.isPending

  async function runLifecycle(action: LifecycleAction) {
    const mutation =
      action === 'open' ? openPosting : action === 'close' ? closePosting : cancelPosting
    try {
      await mutation.mutateAsync(id!)
      toast.success(`Posting ${action === 'cancel' ? 'cancelled' : `${action}ed`}`)
      setDialogAction(null)
    } catch (err) {
      // Surfaces backend lifecycle rejections (e.g. 422 for an invalid transition).
      toast.error(getApiErrorMessage(err))
    }
  }

  async function runTag(fn: () => Promise<unknown>, successMessage: string) {
    try {
      await fn()
      toast.success(successMessage)
    } catch (err) {
      toast.error(getApiErrorMessage(err))
    }
  }

  if (isLoading) {
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

  if (isError || !posting) {
    return (
      <div className="space-y-6">
        <Button variant="ghost" size="sm" onClick={() => navigate(listRoute)}>
          <ChevronLeft className="h-4 w-4" /> Back
        </Button>
        <ErrorState title="Job posting not found" onRetry={() => refetch()} />
      </div>
    )
  }

  const skills = posting.requiredSkills ?? []
  const branches = posting.eligibleBranches ?? []
  const isDraft = posting.status === 'DRAFT'
  const canOpen = posting.status === 'DRAFT'
  const canClose = posting.status === 'OPEN'
  const canCancel = posting.status !== 'CANCELLED'
  const isStudent = user?.role === ROLES.STUDENT

  return (
    <div className="space-y-6">
      <Button variant="ghost" size="sm" onClick={() => navigate(listRoute)}>
        <ChevronLeft className="h-4 w-4" /> Back
      </Button>

      <PageHeader
        title={posting.title}
        description={posting.companyName}
        action={
          isOfficer && isDraft ? (
            <Button asChild variant="outline">
              <Link to={ROUTES.OFFICER.EDIT_JOB_POSTING(posting.id)}>
                <Pencil className="h-4 w-4" /> Edit
              </Link>
            </Button>
          ) : undefined
        }
      />

      <div className="grid gap-4 lg:grid-cols-3">
        <div className="space-y-4 lg:col-span-2">
          <Card>
            <CardHeader>
              <CardTitle>Posting Details</CardTitle>
            </CardHeader>
            <CardContent className="space-y-3">
              <div className="flex items-center justify-between gap-4">
                <span className="text-sm text-muted-foreground">Status</span>
                <StatusBadge status={posting.status} />
              </div>
              <div className="flex items-center justify-between gap-4">
                <span className="text-sm text-muted-foreground shrink-0">Company</span>
                <Link
                  to={ROUTES.OFFICER.COMPANY_DETAIL(posting.companyId)}
                  className="text-sm font-medium text-primary underline-offset-4 hover:underline"
                >
                  {posting.companyName}
                </Link>
              </div>
              <Field label="CTC" value={formatCTCRange(posting.ctcMin, posting.ctcMax)} />
              <Field label="Application deadline" value={formatDate(posting.applicationDeadline)} />
              <Field label="Offer limit" value={String(posting.offerLimit)} />
              <Field label="Created" value={formatDateTime(posting.createdAt)} />
              <Field label="Last updated" value={formatDateTime(posting.updatedAt)} />
              {posting.description && (
                <div className="space-y-1 pt-1">
                  <span className="text-sm text-muted-foreground">Description</span>
                  <p className="whitespace-pre-line text-sm">{posting.description}</p>
                </div>
              )}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Required Skills</CardTitle>
            </CardHeader>
            <CardContent>
              {isOfficer ? (
                <TagSection
                  noun="skill"
                  items={skills}
                  options={skillsQuery.data ?? []}
                  optionsLoading={skillsQuery.isLoading}
                  addPending={addSkill.isPending}
                  removePendingId={removeSkill.isPending ? (removeSkill.variables ?? null) : null}
                  onAdd={(skillId) =>
                    void runTag(() => addSkill.mutateAsync(skillId), 'Skill added')
                  }
                  onRemove={(skillId) =>
                    void runTag(() => removeSkill.mutateAsync(skillId), 'Skill removed')
                  }
                />
              ) : (
                <ReadOnlyTags items={skills} emptyLabel="No specific skills required." />
              )}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Eligible Branches</CardTitle>
            </CardHeader>
            <CardContent>
              {isOfficer ? (
                <TagSection
                  noun="branch"
                  items={branches}
                  options={branchesQuery.data ?? []}
                  optionsLoading={branchesQuery.isLoading}
                  addPending={addBranch.isPending}
                  removePendingId={removeBranch.isPending ? (removeBranch.variables ?? null) : null}
                  onAdd={(branchId) =>
                    void runTag(() => addBranch.mutateAsync(branchId), 'Branch added')
                  }
                  onRemove={(branchId) =>
                    void runTag(() => removeBranch.mutateAsync(branchId), 'Branch removed')
                  }
                />
              ) : (
                <ReadOnlyTags items={branches} emptyLabel="Open to all branches." />
              )}
            </CardContent>
          </Card>
        </div>

        <div className="space-y-4">
          {isOfficer && (
            <Card>
              <CardHeader>
                <CardTitle>Lifecycle</CardTitle>
              </CardHeader>
              <CardContent className="space-y-2">
                {canOpen && (
                  <Button
                    variant="outline"
                    size="sm"
                    className="w-full"
                    onClick={() => setDialogAction('open')}
                    disabled={lifecyclePending}
                  >
                    Open for applications
                  </Button>
                )}
                {canClose && (
                  <Button
                    variant="outline"
                    size="sm"
                    className="w-full"
                    onClick={() => setDialogAction('close')}
                    disabled={lifecyclePending}
                  >
                    Close posting
                  </Button>
                )}
                {canCancel && (
                  <Button
                    variant="destructive"
                    size="sm"
                    className="w-full"
                    onClick={() => setDialogAction('cancel')}
                    disabled={lifecyclePending}
                  >
                    Cancel posting
                  </Button>
                )}
                {!canOpen && !canClose && !canCancel && (
                  <p className="text-sm text-muted-foreground">
                    No lifecycle actions available for a {posting.status.toLowerCase()} posting.
                  </p>
                )}
              </CardContent>
            </Card>
          )}

          {isStudent && (
            <Card>
              <CardHeader>
                <CardTitle>Apply</CardTitle>
              </CardHeader>
              <CardContent className="space-y-2">
                {/*
                  Phase 5 extension point — application submission.
                  Wire this button to the applications feature (POST /api/applications
                  via applicationsApi.create) when Phase 5 lands. Intentionally inert here.
                */}
                <Button className="w-full" disabled title="Applications open in a future release">
                  Apply now
                </Button>
                <p className="text-xs text-muted-foreground">
                  Applications aren’t available yet — this will be enabled soon.
                </p>
              </CardContent>
            </Card>
          )}
        </div>
      </div>

      <ConfirmDialog
        open={dialogAction !== null}
        onOpenChange={(next) => !next && setDialogAction(null)}
        title={dialogAction ? LIFECYCLE_COPY[dialogAction].title : ''}
        description={dialogAction ? LIFECYCLE_COPY[dialogAction].description : ''}
        confirmLabel={dialogAction ? LIFECYCLE_COPY[dialogAction].confirmLabel : ''}
        destructive={dialogAction ? LIFECYCLE_COPY[dialogAction].destructive : false}
        isPending={lifecyclePending}
        onConfirm={() => dialogAction && runLifecycle(dialogAction)}
      />
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

function ReadOnlyTags({
  items,
  emptyLabel,
}: {
  items: { id: string; name: string }[]
  emptyLabel: string
}) {
  if (items.length === 0) {
    return <p className="text-sm text-muted-foreground">{emptyLabel}</p>
  }
  return (
    <ul className="flex flex-wrap gap-2">
      {items.map((item) => (
        <li key={item.id}>
          <Badge variant="secondary">{item.name}</Badge>
        </li>
      ))}
    </ul>
  )
}
