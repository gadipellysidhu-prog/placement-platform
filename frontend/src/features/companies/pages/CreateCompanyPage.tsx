import { useNavigate } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { toast } from 'sonner'
import { ChevronLeft } from 'lucide-react'
import { PageHeader } from '@/shared/ui/page-header'
import { Button } from '@/shared/ui/button'
import { Input } from '@/shared/ui/input'
import { Textarea } from '@/shared/ui/textarea'
import { FormField } from '@/shared/forms/form-field'
import { Spinner } from '@/shared/ui/spinner'
import { Card, CardContent } from '@/shared/ui/card'
import { ROUTES } from '@/constants/routes'
import { getApiErrorMessage } from '@/lib/api'
import { useCreateCompany } from '../hooks/use-companies'

const createCompanySchema = z.object({
  name: z.string().min(1, 'Company name is required').max(255),
  website: z.string().url('Enter a valid URL').optional().or(z.literal('')),
  industry: z.string().max(100).optional().or(z.literal('')),
  description: z.string().optional().or(z.literal('')),
})

type CreateCompanyFormValues = z.infer<typeof createCompanySchema>

export default function CreateCompanyPage() {
  const navigate = useNavigate()
  const { mutateAsync: createCompany, isPending } = useCreateCompany()

  const form = useForm<CreateCompanyFormValues>({
    resolver: zodResolver(createCompanySchema),
    defaultValues: { name: '', website: '', industry: '', description: '' },
  })

  async function onSubmit(values: CreateCompanyFormValues) {
    try {
      const company = await createCompany({
        name: values.name,
        website: values.website || undefined,
        industry: values.industry || undefined,
        description: values.description || undefined,
      })
      toast.success(`${company.name} added successfully`)
      navigate(ROUTES.OFFICER.COMPANY_DETAIL(company.id))
    } catch (err) {
      toast.error(getApiErrorMessage(err))
    }
  }

  return (
    <div className="space-y-6">
      <Button variant="ghost" size="sm" onClick={() => navigate(ROUTES.OFFICER.COMPANIES)}>
        <ChevronLeft className="h-4 w-4" /> Back to companies
      </Button>

      <PageHeader
        title="Add Company"
        description="Register a new company in the placement platform"
      />

      <Card className="max-w-xl">
        <CardContent className="pt-6">
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4" noValidate>
            <FormField label="Company name" error={form.formState.errors.name?.message} required>
              <Input placeholder="e.g. Acme Corp" disabled={isPending} {...form.register('name')} />
            </FormField>

            <FormField label="Industry" error={form.formState.errors.industry?.message}>
              <Input
                placeholder="e.g. Technology"
                disabled={isPending}
                {...form.register('industry')}
              />
            </FormField>

            <FormField label="Website" error={form.formState.errors.website?.message}>
              <Input
                type="url"
                placeholder="https://example.com"
                disabled={isPending}
                {...form.register('website')}
              />
            </FormField>

            <FormField label="Description" error={form.formState.errors.description?.message}>
              <Textarea
                placeholder="Brief description of the company..."
                rows={3}
                disabled={isPending}
                {...form.register('description')}
              />
            </FormField>

            <div className="flex items-center gap-3 pt-2">
              <Button type="submit" disabled={isPending}>
                {isPending ? (
                  <>
                    <Spinner size="sm" className="mr-2" />
                    Creating…
                  </>
                ) : (
                  'Add company'
                )}
              </Button>
              <Button
                type="button"
                variant="outline"
                onClick={() => navigate(ROUTES.OFFICER.COMPANIES)}
                disabled={isPending}
              >
                Cancel
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  )
}
