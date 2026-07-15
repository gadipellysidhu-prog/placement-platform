import { Award } from 'lucide-react'
import { PageHeader } from '@/shared/ui/page-header'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/ui/card'
import { StatusBadge } from '@/shared/ui/status-badge'
import { Skeleton } from '@/shared/ui/skeleton'
import { ErrorState } from '@/shared/ui/error-state'
import { EmptyState } from '@/shared/ui/empty-state'
import { formatDateTime } from '@/utils/format'
import { useMyStudentProfile } from '@/features/students/hooks/use-students'
import { useMyCertificates } from '../hooks/use-certificates'
import { SubmitCertificateDialog } from '../components/SubmitCertificateDialog'
import { CertificateFileLink } from '../components/CertificateFileLink'

export default function MyCertificatesPage() {
  const profile = useMyStudentProfile()
  const { data, isLoading, isError, refetch } = useMyCertificates()

  return (
    <div className="space-y-6">
      <PageHeader
        title="My Certificates"
        description="Submit certificates and track their verification status."
        action={profile.data ? <SubmitCertificateDialog studentId={profile.data.id} /> : undefined}
      />

      {(isLoading || profile.isLoading) && (
        <div className="grid gap-4">
          <Skeleton className="h-28 rounded-xl" />
          <Skeleton className="h-28 rounded-xl" />
        </div>
      )}

      {!profile.isLoading && profile.isError && (
        <ErrorState
          title="Your student profile isn’t set up yet"
          description="Contact your placement officer to create your profile before submitting certificates."
          onRetry={() => profile.refetch()}
        />
      )}

      {!isLoading && isError && (
        <ErrorState title="Couldn’t load your certificates" onRetry={() => refetch()} />
      )}

      {!isLoading && !isError && data && data.length === 0 && (
        <EmptyState
          icon={<Award />}
          title="No certificates yet"
          description="Submit your first certificate to have it verified by a placement officer."
        />
      )}

      {!isLoading && !isError && data && data.length > 0 && (
        <ul className="grid gap-4">
          {data.map((certificate) => (
            <li key={certificate.id}>
              <Card>
                <CardHeader className="flex flex-row items-start justify-between gap-4 space-y-0">
                  <div className="min-w-0 space-y-1">
                    <CardTitle className="truncate">{certificate.name}</CardTitle>
                    <p className="truncate text-sm text-muted-foreground">
                      {certificate.issuingOrganization ?? 'Unknown issuer'}
                      {certificate.skillName && ` · ${certificate.skillName}`}
                    </p>
                  </div>
                  <StatusBadge status={certificate.verificationStatus} />
                </CardHeader>
                <CardContent className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                  <p className="text-xs text-muted-foreground">
                    Submitted {formatDateTime(certificate.createdAt)}
                  </p>
                  <CertificateFileLink fileKey={certificate.fileKey} />
                </CardContent>
              </Card>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
