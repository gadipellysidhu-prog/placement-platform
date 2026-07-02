import { useParams, useNavigate } from 'react-router-dom'
import { ChevronLeft, RefreshCw } from 'lucide-react'
import { toast } from 'sonner'
import { PageHeader } from '@/shared/ui/page-header'
import { StatusBadge } from '@/shared/ui/status-badge'
import { ErrorState } from '@/shared/ui/error-state'
import { Skeleton } from '@/shared/ui/skeleton'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/ui/card'
import { Badge } from '@/shared/ui/badge'
import { Button } from '@/shared/ui/button'
import { ROUTES } from '@/constants/routes'
import { getApiErrorMessage } from '@/lib/api'
import {
  useStudent,
  useUpdateStudentStatus,
  useUpdateStudentEligibility,
} from '../hooks/use-students'
import type { StudentResponse } from '@/lib/api'

const STATUS_TRANSITIONS: Record<StudentResponse['status'], StudentResponse['status'][]> = {
  ACTIVE: ['PLACED', 'OPTED_OUT', 'BLOCKED', 'GRADUATED'],
  PLACED: ['GRADUATED'],
  OPTED_OUT: ['ACTIVE'],
  BLOCKED: ['ACTIVE'],
  GRADUATED: [],
}

export default function StudentDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()

  const { data: student, isLoading, isError, refetch } = useStudent(id!)

  const updateStatus = useUpdateStudentStatus(id!)
  const updateEligibility = useUpdateStudentEligibility(id!)

  async function handleStatusChange(status: StudentResponse['status']) {
    try {
      await updateStatus.mutateAsync({ status })
      toast.success(`Status updated to ${status.replace('_', ' ').toLowerCase()}`)
    } catch (err) {
      toast.error(getApiErrorMessage(err))
    }
  }

  async function handleEligibilityRefresh() {
    try {
      await updateEligibility.mutateAsync()
      toast.success('Eligibility re-evaluated')
    } catch (err) {
      toast.error(getApiErrorMessage(err))
    }
  }

  if (isLoading) {
    return (
      <div className="space-y-6">
        <Skeleton className="h-9 w-64" />
        <div className="grid gap-4 md:grid-cols-2">
          <Skeleton className="h-52 rounded-xl" />
          <Skeleton className="h-52 rounded-xl" />
        </div>
      </div>
    )
  }

  if (isError || !student) {
    return (
      <div className="space-y-6">
        <Button variant="ghost" size="sm" onClick={() => navigate(ROUTES.OFFICER.STUDENTS)}>
          <ChevronLeft className="h-4 w-4" /> Back to students
        </Button>
        <ErrorState title="Student not found" onRetry={() => refetch()} />
      </div>
    )
  }

  const availableTransitions = STATUS_TRANSITIONS[student.status]

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-2">
        <Button variant="ghost" size="sm" onClick={() => navigate(ROUTES.OFFICER.STUDENTS)}>
          <ChevronLeft className="h-4 w-4" /> Back to students
        </Button>
      </div>

      <PageHeader title={student.rollNumber} description={student.userEmail} />

      <div className="grid gap-4 md:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>Student Information</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            <Field label="Email" value={student.userEmail} />
            <Field label="Roll Number" value={student.rollNumber} />
            <Field label="Branch" value={student.branchName ?? '—'} />
            <Field label="Current Year" value={`Year ${student.currentYear}`} />
            <Field label="CGPA" value={student.cgpa != null ? String(student.cgpa) : '—'} />
            <div className="flex items-center justify-between gap-4">
              <span className="text-sm text-muted-foreground">Status</span>
              <StatusBadge status={student.status} />
            </div>
            <div className="flex items-center justify-between gap-4">
              <span className="text-sm text-muted-foreground">Eligible</span>
              <Badge variant={student.placementEligible ? 'success' : 'outline'}>
                {student.placementEligible ? 'Yes' : 'No'}
              </Badge>
            </div>
          </CardContent>
        </Card>

        <div className="space-y-4">
          {student.skillNames.length > 0 && (
            <Card>
              <CardHeader>
                <CardTitle>Skills</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="flex flex-wrap gap-1.5">
                  {student.skillNames.map((skill) => (
                    <Badge key={skill} variant="secondary">
                      {skill}
                    </Badge>
                  ))}
                </div>
              </CardContent>
            </Card>
          )}

          <Card>
            <CardHeader>
              <CardTitle>Actions</CardTitle>
            </CardHeader>
            <CardContent className="space-y-3">
              <div className="space-y-2">
                <p className="text-xs font-medium text-muted-foreground uppercase tracking-wide">
                  Change Status
                </p>
                <div className="flex flex-wrap gap-2">
                  {availableTransitions.length === 0 ? (
                    <p className="text-sm text-muted-foreground">No transitions available</p>
                  ) : (
                    availableTransitions.map((s) => (
                      <Button
                        key={s}
                        size="sm"
                        variant="outline"
                        onClick={() => handleStatusChange(s)}
                        disabled={updateStatus.isPending}
                      >
                        {s.replace('_', ' ')}
                      </Button>
                    ))
                  )}
                </div>
              </div>

              <div className="space-y-2">
                <p className="text-xs font-medium text-muted-foreground uppercase tracking-wide">
                  Eligibility
                </p>
                <Button
                  size="sm"
                  variant="outline"
                  onClick={handleEligibilityRefresh}
                  disabled={updateEligibility.isPending}
                >
                  <RefreshCw className="h-3.5 w-3.5" />
                  Re-evaluate eligibility
                </Button>
              </div>
            </CardContent>
          </Card>
        </div>
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
