import { useState } from 'react'
import { Check, X } from 'lucide-react'
import { Button } from '@/shared/ui/button'
import { ConfirmDialog } from '@/features/job-postings/components/ConfirmDialog'
import { getApiErrorMessage, type CertificateResponse } from '@/lib/api'
import { useToast } from '@/shared/hooks/use-toast'
import { useVerifyCertificate, useRejectCertificate } from '../hooks/use-certificates'

interface CertificateReviewActionsProps {
  certificate: CertificateResponse
}

/**
 * Officer verify / reject controls for a certificate. Both actions require a
 * confirmation and are only offered while the certificate is PENDING — the backend
 * rejects transitions from a terminal state with 422, which is also surfaced as a toast
 * if it happens due to a race.
 */
export function CertificateReviewActions({ certificate }: CertificateReviewActionsProps) {
  const [confirm, setConfirm] = useState<'verify' | 'reject' | null>(null)
  const verify = useVerifyCertificate()
  const reject = useRejectCertificate()
  const toast = useToast()

  if (certificate.verificationStatus !== 'PENDING') {
    return <span className="text-xs text-muted-foreground">No actions</span>
  }

  const isPending = verify.isPending || reject.isPending

  async function handleVerify() {
    try {
      await verify.mutateAsync(certificate.id)
      toast.success('Certificate verified')
      setConfirm(null)
    } catch (err) {
      toast.error('Could not verify', getApiErrorMessage(err))
    }
  }

  async function handleReject() {
    try {
      await reject.mutateAsync(certificate.id)
      toast.success('Certificate rejected')
      setConfirm(null)
    } catch (err) {
      toast.error('Could not reject', getApiErrorMessage(err))
    }
  }

  return (
    <div className="flex items-center gap-2">
      <Button
        type="button"
        variant="outline"
        size="sm"
        onClick={() => setConfirm('verify')}
        disabled={isPending}
      >
        <Check className="h-4 w-4" /> Verify
      </Button>
      <Button
        type="button"
        variant="ghost"
        size="sm"
        onClick={() => setConfirm('reject')}
        disabled={isPending}
      >
        <X className="h-4 w-4" /> Reject
      </Button>

      <ConfirmDialog
        open={confirm === 'verify'}
        onOpenChange={(next) => !next && setConfirm(null)}
        title="Verify this certificate?"
        description={`“${certificate.name}” will be marked as verified for ${certificate.studentRollNumber}.`}
        confirmLabel="Verify certificate"
        isPending={verify.isPending}
        onConfirm={handleVerify}
      />
      <ConfirmDialog
        open={confirm === 'reject'}
        onOpenChange={(next) => !next && setConfirm(null)}
        title="Reject this certificate?"
        description={`“${certificate.name}” will be marked as rejected for ${certificate.studentRollNumber}. This cannot be undone.`}
        confirmLabel="Reject certificate"
        destructive
        isPending={reject.isPending}
        onConfirm={handleReject}
      />
    </div>
  )
}
