import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Plus } from 'lucide-react'
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
import { FileUploadField } from '@/shared/ui/file-upload-field'
import { useToast } from '@/shared/hooks/use-toast'
import { getApiErrorMessage, type FileResponse } from '@/lib/api'
import { useSubmitCertificate } from '../hooks/use-certificates'
import { certificateFormSchema, type CertificateFormValues } from '../schemas/certificate.schema'

interface SubmitCertificateDialogProps {
  studentId: string
}

/**
 * Student certificate submission. Collects a name + optional issuer, requires a scanned
 * document via {@link FileUploadField}, and only enables submission once the upload has
 * completed. The uploaded file's id is sent as the certificate `fileKey`.
 */
export function SubmitCertificateDialog({ studentId }: SubmitCertificateDialogProps) {
  const [open, setOpen] = useState(false)
  const [file, setFile] = useState<FileResponse | null>(null)
  const toast = useToast()
  const submit = useSubmitCertificate()

  const form = useForm<CertificateFormValues>({
    resolver: zodResolver(certificateFormSchema),
    defaultValues: { name: '', issuingOrganization: '' },
  })

  function resetAll() {
    form.reset()
    setFile(null)
  }

  async function onSubmit(values: CertificateFormValues) {
    if (!file) return
    try {
      await submit.mutateAsync({
        studentId,
        name: values.name,
        issuingOrganization: values.issuingOrganization || undefined,
        fileKey: file.id,
      })
      toast.success('Certificate submitted', 'It is now pending verification.')
      resetAll()
      setOpen(false)
    } catch (err) {
      toast.error('Submission failed', getApiErrorMessage(err))
    }
  }

  return (
    <Dialog
      open={open}
      onOpenChange={(next) => {
        if (submit.isPending) return
        setOpen(next)
        if (!next) resetAll()
      }}
    >
      <DialogTrigger asChild>
        <Button>
          <Plus className="h-4 w-4" /> Submit certificate
        </Button>
      </DialogTrigger>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Submit a certificate</DialogTitle>
          <DialogDescription>
            Upload your certificate document and add its details. A placement officer will verify
            it.
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4" noValidate>
          <FormField label="Certificate name" required error={form.formState.errors.name?.message}>
            <Input
              placeholder="e.g. AWS Certified Developer"
              disabled={submit.isPending}
              {...form.register('name')}
            />
          </FormField>

          <FormField
            label="Issuing organization"
            error={form.formState.errors.issuingOrganization?.message}
            hint="Optional"
          >
            <Input
              placeholder="e.g. Amazon Web Services"
              disabled={submit.isPending}
              {...form.register('issuingOrganization')}
            />
          </FormField>

          <FormField label="Document" required>
            <FileUploadField value={file} onChange={setFile} disabled={submit.isPending} />
          </FormField>

          <DialogFooter>
            <Button
              type="button"
              variant="outline"
              onClick={() => setOpen(false)}
              disabled={submit.isPending}
            >
              Cancel
            </Button>
            <Button type="submit" disabled={!file || submit.isPending}>
              {submit.isPending ? (
                <>
                  <Spinner size="sm" className="mr-2" /> Submitting…
                </>
              ) : (
                'Submit'
              )}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
