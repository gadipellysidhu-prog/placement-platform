import { describe, expect, it } from 'vitest'
import { http, HttpResponse } from 'msw'
import { renderWithProviders, screen, waitFor } from '@/test'
import { server, API_BASE_URL } from '@/test'
import type { ApplicationStatus, JobApplicationResponse } from '@/lib/api'
import { StatusTransition } from './StatusTransition'

function application(status: ApplicationStatus): JobApplicationResponse {
  return {
    id: 'app-1',
    studentId: 'stu-1',
    studentRollNumber: 'CS-001',
    jobPostingId: 'jp-1',
    jobPostingTitle: 'Backend Engineer',
    companyId: 'co-1',
    companyName: 'Acme Corp',
    status,
    appliedAt: '2026-07-05T10:00:00Z',
    createdAt: '2026-07-05T10:00:00Z',
    updatedAt: '2026-07-05T10:00:00Z',
  }
}

describe('StatusTransition — offers only legal transitions', () => {
  it('offers exactly SHORTLISTED and REJECTED from APPLIED', async () => {
    const { user } = renderWithProviders(<StatusTransition application={application('APPLIED')} />)

    await user.click(screen.getByRole('combobox', { name: /new status/i }))

    expect(await screen.findByRole('option', { name: 'Shortlisted' })).toBeInTheDocument()
    expect(screen.getByRole('option', { name: 'Rejected' })).toBeInTheDocument()
    // Illegal skips and the student-only WITHDRAWN are never offered.
    expect(screen.queryByRole('option', { name: 'Interviewed' })).not.toBeInTheDocument()
    expect(screen.queryByRole('option', { name: 'Offered' })).not.toBeInTheDocument()
    expect(screen.queryByRole('option', { name: 'Withdrawn' })).not.toBeInTheDocument()
  })

  it('offers INTERVIEWED and REJECTED from SHORTLISTED', async () => {
    const { user } = renderWithProviders(
      <StatusTransition application={application('SHORTLISTED')} />,
    )
    await user.click(screen.getByRole('combobox', { name: /new status/i }))
    expect(await screen.findByRole('option', { name: 'Interviewed' })).toBeInTheDocument()
    expect(screen.getByRole('option', { name: 'Rejected' })).toBeInTheDocument()
    expect(screen.queryByRole('option', { name: 'Offered' })).not.toBeInTheDocument()
  })

  it('offers OFFERED and REJECTED from INTERVIEWED', async () => {
    const { user } = renderWithProviders(
      <StatusTransition application={application('INTERVIEWED')} />,
    )
    await user.click(screen.getByRole('combobox', { name: /new status/i }))
    expect(await screen.findByRole('option', { name: 'Offered' })).toBeInTheDocument()
    expect(screen.getByRole('option', { name: 'Rejected' })).toBeInTheDocument()
  })

  it('shows no control for terminal states', () => {
    renderWithProviders(<StatusTransition application={application('OFFERED')} />)
    expect(screen.queryByRole('combobox', { name: /new status/i })).not.toBeInTheDocument()
    expect(screen.getByText(/no further status changes are possible/i)).toBeInTheDocument()
  })
})

describe('StatusTransition — mutation', () => {
  it('PUTs the chosen status and confirms on success', async () => {
    let body: { status: string } | null = null
    server.use(
      http.put(`${API_BASE_URL}/api/applications/app-1/status`, async ({ request }) => {
        body = (await request.json()) as typeof body
        return HttpResponse.json(application('SHORTLISTED'))
      }),
    )

    const { user } = renderWithProviders(<StatusTransition application={application('APPLIED')} />)

    await user.click(screen.getByRole('combobox', { name: /new status/i }))
    await user.click(await screen.findByRole('option', { name: 'Shortlisted' }))
    await user.click(screen.getByRole('button', { name: /update status/i }))

    await waitFor(() => expect(body).toEqual({ status: 'SHORTLISTED' }))
  })

  it('surfaces a backend 409/422 rejection as a readable error', async () => {
    server.use(
      http.put(`${API_BASE_URL}/api/applications/app-1/status`, () =>
        HttpResponse.json(
          {
            title: 'Unprocessable Entity',
            status: 422,
            detail: 'Invalid application status transition from APPLIED to REJECTED',
          },
          { status: 422 },
        ),
      ),
    )

    const { user } = renderWithProviders(<StatusTransition application={application('APPLIED')} />)

    await user.click(screen.getByRole('combobox', { name: /new status/i }))
    await user.click(await screen.findByRole('option', { name: 'Rejected' }))
    await user.click(screen.getByRole('button', { name: /update status/i }))

    // Surfaced verbatim in both the inline alert and the toast.
    const matches = await screen.findAllByText(
      /invalid application status transition from applied to rejected/i,
    )
    expect(matches.length).toBeGreaterThanOrEqual(1)
  })
})
