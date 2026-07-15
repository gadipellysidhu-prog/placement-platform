import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Gift } from 'lucide-react'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/shared/ui/dialog'
import { Button } from '@/shared/ui/button'
import { Input } from '@/shared/ui/input'
import { Spinner } from '@/shared/ui/spinner'
import { FormField } from '@/shared/forms/form-field'
import { useToast } from '@/shared/hooks/use-toast'
import { getApiErrorMessage } from '@/lib/api'
import { useCreateOffer } from '../hooks/use-offers'
import {
  offerFormSchema,
  toCreateOfferPayload,
  type OfferFormValues,
} from '../schemas/offer.schema'

interface CreateOfferDialogProps {
  applicationId: string
  /** Shown in the dialog for context (company / candidate). */
  label?: string
}

/**
 * Officer offer-creation launched from an application detail. The backend only allows
 * an offer for an application in OFFERED status and rejects a duplicate with 409 — both
 * are surfaced as a readable toast without breaking the UX (the dialog stays open so the
 * officer can retry or cancel).
 */
export function CreateOfferDialog({ applicationId, label }: CreateOfferDialogProps) {
  const [open, setOpen] = useState(false)
  const create = useCreateOffer()
  const toast = useToast()

  const form = useForm<OfferFormValues>({
    resolver: zodResolver(offerFormSchema),
    defaultValues: { ctc: '', joiningDate: '' },
  })

  async function onSubmit(values: OfferFormValues) {
    try {
      await create.mutateAsync(toCreateOfferPayload(applicationId, values))
      toast.success('Offer created', 'The student can now accept or reject it.')
      form.reset()
      setOpen(false)
    } catch (err) {
      // 409 (duplicate) / 422 (not offerable) — keep the dialog open, show why.
      toast.error('Could not create offer', getApiErrorMessage(err))
    }
  }

  return (
    <Dialog
      open={open}
      onOpenChange={(next) => {
        if (create.isPending) return
        setOpen(next)
        if (!next) form.reset()
      }}
    >
      <DialogTrigger asChild>
        <Button className="w-full">
          <Gift className="h-4 w-4" /> Create offer
        </Button>
      </DialogTrigger>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Create placement offer</DialogTitle>
          <DialogDescription>
            {label ? `Extend an offer for ${label}.` : 'Extend an offer for this application.'} CTC
            and joining date are optional.
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4" noValidate>
          <FormField
            label="CTC (LPA)"
            error={form.formState.errors.ctc?.message}
            hint="Optional — e.g. 12.5"
          >
            <Input
              type="number"
              step="0.1"
              min="0"
              inputMode="decimal"
              placeholder="12.5"
              disabled={create.isPending}
              {...form.register('ctc')}
            />
          </FormField>

          <FormField
            label="Joining date"
            error={form.formState.errors.joiningDate?.message}
            hint="Optional"
          >
            <Input type="date" disabled={create.isPending} {...form.register('joiningDate')} />
          </FormField>

          <DialogFooter>
            <Button
              type="button"
              variant="outline"
              onClick={() => setOpen(false)}
              disabled={create.isPending}
            >
              Cancel
            </Button>
            <Button type="submit" disabled={create.isPending}>
              {create.isPending ? (
                <>
                  <Spinner size="sm" className="mr-2" /> Creating…
                </>
              ) : (
                'Create offer'
              )}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
