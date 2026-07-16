import { useState } from 'react'
import { Clock } from 'lucide-react'
import { Button } from '@/shared/ui/button'
import { ConfirmDialog } from '@/shared/ui/confirm-dialog'
import { getApiErrorMessage, type OfferResponse } from '@/lib/api'
import { useToast } from '@/shared/hooks/use-toast'
import { useExpireOffer } from '../hooks/use-offers'

interface ExpireOfferActionProps {
  offer: OfferResponse
}

/**
 * Officer control to expire a PENDING offer. Only rendered while the offer is PENDING —
 * the backend rejects expiring a terminal offer with 422, surfaced as a toast if it
 * happens due to a race.
 */
export function ExpireOfferAction({ offer }: ExpireOfferActionProps) {
  const [open, setOpen] = useState(false)
  const expire = useExpireOffer()
  const toast = useToast()

  if (offer.status !== 'PENDING') {
    return <span className="text-xs text-muted-foreground">No actions</span>
  }

  async function handleExpire() {
    try {
      await expire.mutateAsync(offer.id)
      toast.success('Offer expired')
      setOpen(false)
    } catch (err) {
      toast.error('Could not expire offer', getApiErrorMessage(err))
    }
  }

  return (
    <>
      <Button variant="ghost" size="sm" onClick={() => setOpen(true)} disabled={expire.isPending}>
        <Clock className="h-4 w-4" /> Expire
      </Button>
      <ConfirmDialog
        open={open}
        onOpenChange={(next) => !expire.isPending && setOpen(next)}
        title="Expire this offer?"
        description={`The pending offer to ${offer.studentRollNumber} (${offer.companyName}) will be marked expired. This cannot be undone.`}
        confirmLabel="Expire offer"
        destructive
        isPending={expire.isPending}
        onConfirm={handleExpire}
      />
    </>
  )
}
