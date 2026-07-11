import { Controller, useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Button } from '@/shared/ui/button'
import { Input } from '@/shared/ui/input'
import { Textarea } from '@/shared/ui/textarea'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/shared/ui/select'
import { FormField } from '@/shared/forms/form-field'
import { Spinner } from '@/shared/ui/spinner'
import { jobPostingFormSchema, type JobPostingFormValues } from '../job-posting.schema'
import { useCompaniesLookup } from '../hooks/use-lookups'

export interface JobPostingFormDefaults {
  companyId?: string
  title?: string
  description?: string
  ctcMin?: string
  ctcMax?: string
  applicationDeadline?: string
  offerLimit?: string
}

interface JobPostingFormProps {
  mode: 'create' | 'edit'
  defaultValues?: JobPostingFormDefaults
  /** Company name shown read-only in edit mode (the owning company is fixed at creation). */
  companyName?: string
  isSubmitting: boolean
  submitLabel: string
  onSubmit: (values: JobPostingFormValues) => void | Promise<void>
  onCancel: () => void
}

const EMPTY_DEFAULTS: Required<JobPostingFormDefaults> = {
  companyId: '',
  title: '',
  description: '',
  ctcMin: '',
  ctcMax: '',
  applicationDeadline: '',
  offerLimit: '',
}

export function JobPostingForm({
  mode,
  defaultValues,
  companyName,
  isSubmitting,
  submitLabel,
  onSubmit,
  onCancel,
}: JobPostingFormProps) {
  const isCreate = mode === 'create'

  const {
    register,
    handleSubmit,
    control,
    formState: { errors },
  } = useForm<JobPostingFormValues>({
    resolver: zodResolver(jobPostingFormSchema),
    defaultValues: { ...EMPTY_DEFAULTS, ...defaultValues },
  })

  // Company selector is only relevant when creating; the picker is disabled during
  // submission and only mounted in create mode to avoid fetching the roster on edit.
  const companiesQuery = useCompaniesLookup()
  const companies = companiesQuery.data?.content ?? []

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
      {isCreate ? (
        <FormField label="Company" error={errors.companyId?.message} required>
          <Controller
            control={control}
            name="companyId"
            render={({ field }) => (
              <Select
                value={field.value || undefined}
                onValueChange={field.onChange}
                disabled={isSubmitting || companiesQuery.isLoading}
              >
                <SelectTrigger aria-label="Company" aria-invalid={!!errors.companyId}>
                  <SelectValue
                    placeholder={
                      companiesQuery.isLoading ? 'Loading companies…' : 'Select a company'
                    }
                  />
                </SelectTrigger>
                <SelectContent>
                  {companies.map((c) => (
                    <SelectItem key={c.id} value={c.id}>
                      {c.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            )}
          />
        </FormField>
      ) : (
        <FormField label="Company">
          <Input value={companyName ?? ''} disabled readOnly />
        </FormField>
      )}

      <FormField label="Title" error={errors.title?.message} required>
        <Input placeholder="e.g. Backend Engineer" disabled={isSubmitting} {...register('title')} />
      </FormField>

      <FormField label="Description" error={errors.description?.message}>
        <Textarea
          placeholder="Role summary, responsibilities, requirements…"
          rows={4}
          disabled={isSubmitting}
          {...register('description')}
        />
      </FormField>

      <div className="grid gap-4 sm:grid-cols-2">
        <FormField label="Minimum CTC (LPA)" error={errors.ctcMin?.message}>
          <Input
            type="number"
            inputMode="decimal"
            min={0}
            step="0.1"
            placeholder="e.g. 8"
            disabled={isSubmitting}
            {...register('ctcMin')}
          />
        </FormField>

        <FormField label="Maximum CTC (LPA)" error={errors.ctcMax?.message}>
          <Input
            type="number"
            inputMode="decimal"
            min={0}
            step="0.1"
            placeholder="e.g. 12"
            disabled={isSubmitting}
            {...register('ctcMax')}
          />
        </FormField>
      </div>

      <div className="grid gap-4 sm:grid-cols-2">
        <FormField label="Application deadline" error={errors.applicationDeadline?.message}>
          <Input type="date" disabled={isSubmitting} {...register('applicationDeadline')} />
        </FormField>

        <FormField
          label="Offer limit"
          error={errors.offerLimit?.message}
          hint="Maximum number of offers"
          required
        >
          <Input
            type="number"
            inputMode="numeric"
            min={1}
            step="1"
            placeholder="e.g. 5"
            disabled={isSubmitting}
            {...register('offerLimit')}
          />
        </FormField>
      </div>

      <div className="flex items-center gap-3 pt-2">
        <Button type="submit" disabled={isSubmitting}>
          {isSubmitting ? (
            <>
              <Spinner size="sm" className="mr-2" />
              Saving…
            </>
          ) : (
            submitLabel
          )}
        </Button>
        <Button type="button" variant="outline" onClick={onCancel} disabled={isSubmitting}>
          Cancel
        </Button>
      </div>
    </form>
  )
}
