import { useParams, useNavigate } from 'react-router-dom'
import { ChevronLeft, ExternalLink } from 'lucide-react'
import { toast } from 'sonner'
import { PageHeader } from '@/shared/ui/page-header'
import { StatusBadge } from '@/shared/ui/status-badge'
import { ErrorState } from '@/shared/ui/error-state'
import { Skeleton } from '@/shared/ui/skeleton'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/ui/card'
import { Button } from '@/shared/ui/button'
import { ROUTES } from '@/constants/routes'
import { ROLES } from '@/constants/roles'
import { getApiErrorMessage } from '@/lib/api'
import { useAuthStore } from '@/stores/auth.store'
import {
  useCompany,
  useActivateCompany,
  useDeactivateCompany,
  useBlacklistCompany,
} from '../hooks/use-companies'

export default function CompanyDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const user = useAuthStore((s) => s.user)

  const { data: company, isLoading, isError, refetch } = useCompany(id!)

  const activate = useActivateCompany()
  const deactivate = useDeactivateCompany()
  const blacklist = useBlacklistCompany()

  const isOfficer = user?.role === ROLES.PLACEMENT_OFFICER || user?.role === ROLES.ADMIN
  const isAdmin = user?.role === ROLES.ADMIN

  async function handleActivate() {
    try {
      await activate.mutateAsync(id!)
      toast.success('Company activated')
    } catch (err) {
      toast.error(getApiErrorMessage(err))
    }
  }

  async function handleDeactivate() {
    try {
      await deactivate.mutateAsync(id!)
      toast.success('Company deactivated')
    } catch (err) {
      toast.error(getApiErrorMessage(err))
    }
  }

  async function handleBlacklist() {
    try {
      await blacklist.mutateAsync(id!)
      toast.success('Company blacklisted')
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

  if (isError || !company) {
    return (
      <div className="space-y-6">
        <Button variant="ghost" size="sm" onClick={() => navigate(ROUTES.OFFICER.COMPANIES)}>
          <ChevronLeft className="h-4 w-4" /> Back to companies
        </Button>
        <ErrorState title="Company not found" onRetry={() => refetch()} />
      </div>
    )
  }

  const isActionPending = activate.isPending || deactivate.isPending || blacklist.isPending

  return (
    <div className="space-y-6">
      <Button variant="ghost" size="sm" onClick={() => navigate(ROUTES.OFFICER.COMPANIES)}>
        <ChevronLeft className="h-4 w-4" /> Back to companies
      </Button>

      <PageHeader title={company.name} description={company.industry ?? undefined} />

      <div className="grid gap-4 md:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>Company Details</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            <div className="flex items-center justify-between gap-4">
              <span className="text-sm text-muted-foreground">Status</span>
              <StatusBadge status={company.status} />
            </div>
            <Field label="Industry" value={company.industry ?? '—'} />
            {company.website && (
              <div className="flex items-center justify-between gap-4">
                <span className="text-sm text-muted-foreground shrink-0">Website</span>
                <a
                  href={company.website}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="flex items-center gap-1 text-sm text-primary underline-offset-4 hover:underline"
                >
                  {company.website.replace(/^https?:\/\//, '')}
                  <ExternalLink className="h-3 w-3" />
                </a>
              </div>
            )}
            {company.description && (
              <div className="space-y-1">
                <span className="text-sm text-muted-foreground">Description</span>
                <p className="text-sm">{company.description}</p>
              </div>
            )}
          </CardContent>
        </Card>

        {isOfficer && (
          <Card>
            <CardHeader>
              <CardTitle>Actions</CardTitle>
            </CardHeader>
            <CardContent className="space-y-2">
              {company.status !== 'ACTIVE' && (
                <Button
                  variant="outline"
                  size="sm"
                  className="w-full"
                  onClick={handleActivate}
                  disabled={isActionPending}
                >
                  Activate
                </Button>
              )}
              {company.status === 'ACTIVE' && (
                <Button
                  variant="outline"
                  size="sm"
                  className="w-full"
                  onClick={handleDeactivate}
                  disabled={isActionPending}
                >
                  Deactivate
                </Button>
              )}
              {isAdmin && company.status !== 'BLACKLISTED' && (
                <Button
                  variant="destructive"
                  size="sm"
                  className="w-full"
                  onClick={handleBlacklist}
                  disabled={isActionPending}
                >
                  Blacklist
                </Button>
              )}
            </CardContent>
          </Card>
        )}
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
