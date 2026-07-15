import { useState } from 'react'
import { Check, X } from 'lucide-react'
import { Button } from '@/shared/ui/button'
import { ConfirmDialog } from '@/features/job-postings/components/ConfirmDialog'
import { formatCTC } from '@/utils/format'
import { getApiErrorMessage, type OfferResponse } from '@/lib/api'
import { useToast } from '@/shared/hooks/use-toast'
import { useAcceptOffer, useRejectOffer } from '../hooks/use-offers'

interface OfferActionsProps {
  offer: OfferResponse
  /** Full-width buttons (used in the card footer on mobile). */
  fullWidth?: boolean
}

/**
 * Student accept / reject controls for a PENDING offer. Accepting is a binding action
 * (it marks the student PLACED server-side) so it is gated behind a strong confirmation.
 * Once an offer is ACCEPTED / REJECTED / EXPIRED it is terminal and no actions render.
 */
export function OfferActions({ offer, fullWidth }: OfferActionsProps) {
  const [confirm, setConfirm] = useState<'accept' | 'reject' | null>(null)
  const accept = useAcceptOffer()
  const reject = useRejectOffer()
  const toast = useToast()

  if (offer.status !== 'PENDING') {
    return null
  }

  const isPending = accept.isPending || reject.isPending

  async function handleAccept() {
    try {
      await accept.mutateAsync(offer.id)
      toast.success('Offer accepted', `Congratulations! You’re placed at ${offer.companyName}.`)
      setConfirm(null)
    } catch (err) {
      toast.error('Could not accept offer', getApiErrorMessage(err))
    }
  }

  async function handleReject() {
    try {
      await reject.mutateAsync(offer.id)
      toast.success('Offer rejected')
      setConfirm(null)
    } catch (err) {
      toast.error('Could not reject offer', getApiErrorMessage(err))
    }
  }

  return (
    <div className={fullWidth ? 'grid grid-cols-2 gap-2' : 'flex items-center gap-2'}>
      <Button
        size="sm"
        className={fullWidth ? 'w-full' : undefined}
        onClick={() => setConfirm('accept')}
        disabled={isPending}
      >
        <Check className="h-4 w-4" /> Accept
      </Button>
      <Button
        variant="outline"
        size="sm"
        className={fullWidth ? 'w-full' : undefined}
        onClick={() => setConfirm('reject')}
        disabled={isPending}
      >
        <X className="h-4 w-4" /> Reject
      </Button>

      <ConfirmDialog
        open={confirm === 'accept'}
        onOpenChange={(next) => !next && setConfirm(null)}
        title={`Accept the offer from ${offer.companyName}?`}
        description={`Accepting is binding: you’ll be marked as placed${
          offer.ctc != null ? ` at ${formatCTC(offer.ctc)}` : ''
        }. This can’t be undone, and any other pending offers should be declined.`}
        confirmLabel="Yes, accept offer"
        isPending={accept.isPending}
        onConfirm={handleAccept}
      />
      <ConfirmDialog
        open={confirm === 'reject'}
        onOpenChange={(next) => !next && setConfirm(null)}
        title={`Reject the offer from ${offer.companyName}?`}
        description="This declines the offer permanently. You won’t be able to accept it later."
        confirmLabel="Reject offer"
        destructive
        isPending={reject.isPending}
        onConfirm={handleReject}
      />
    </div>
  )
}
