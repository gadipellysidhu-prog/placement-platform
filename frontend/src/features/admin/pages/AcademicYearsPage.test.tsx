import { describe, expect, it } from 'vitest'
import { http, HttpResponse } from 'msw'
import { renderWithProviders, screen, server, API_BASE_URL, pageOf, mockAcademicYear } from '@/test'
import AcademicYearsPage from './AcademicYearsPage'

describe('AcademicYearsPage', () => {
  it('lists years and badges the active one', async () => {
    renderWithProviders(<AcademicYearsPage />)

    expect(await screen.findByText('2026-27')).toBeInTheDocument()
    expect(screen.getByText('2025-26')).toBeInTheDocument()
    // Exactly one year is active, so exactly one badge.
    expect(screen.getAllByText('Active')).toHaveLength(1)
  })

  it('only offers Activate for a year that is not already active', async () => {
    renderWithProviders(<AcademicYearsPage />)
    await screen.findByText('2025-26')

    expect(screen.getAllByRole('button', { name: 'Activate' })).toHaveLength(1)
  })

  it('names the year being replaced before switching the active season', async () => {
    const { user } = renderWithProviders(<AcademicYearsPage />)
    await screen.findByText('2025-26')

    await user.click(screen.getByRole('button', { name: 'Activate' }))

    // The backend enforces a single active year, so activation is a switch —
    // the confirmation must say what is being deactivated.
    expect(
      await screen.findByText('2025-26 becomes the active placement season, replacing 2026-27.'),
    ).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Activate year' }))
    expect(await screen.findByText('2025-26 is now the active year.')).toBeInTheDocument()
  })

  it('creates a year', async () => {
    const { user } = renderWithProviders(<AcademicYearsPage />)
    await screen.findByText('2026-27')

    await user.click(screen.getByRole('button', { name: /new year/i }))
    await user.type(await screen.findByLabelText('Label'), '2027-28')
    await user.type(screen.getByLabelText('Start date'), '2027-07-01')
    await user.type(screen.getByLabelText('End date'), '2028-06-30')
    await user.click(screen.getByRole('button', { name: 'Save' }))

    expect(await screen.findByText('Academic year created.')).toBeInTheDocument()
  })

  it('fixes the label when editing, since the backend updates dates only', async () => {
    const { user } = renderWithProviders(<AcademicYearsPage />)
    await screen.findByText('2026-27')

    await user.click(screen.getByLabelText('Edit 2026-27'))

    expect(await screen.findByLabelText('Label')).toBeDisabled()
    expect(screen.getByLabelText('Start date')).toBeEnabled()
  })

  it('sends only the date range when updating', async () => {
    let body: unknown = null
    server.use(
      http.put(`${API_BASE_URL}/api/academic-years/:id`, async ({ request }) => {
        body = await request.json()
        return HttpResponse.json(mockAcademicYear)
      }),
    )

    const { user } = renderWithProviders(<AcademicYearsPage />)
    await screen.findByText('2026-27')

    await user.click(screen.getByLabelText('Edit 2026-27'))
    await user.click(await screen.findByRole('button', { name: 'Save' }))
    await screen.findByText('Academic year updated.')

    expect(body).toEqual({ startDate: '2026-07-01', endDate: '2027-06-30' })
  })

  it('surfaces a backend rejection instead of reporting success', async () => {
    server.use(
      http.post(`${API_BASE_URL}/api/academic-years`, () =>
        HttpResponse.json(
          { title: 'Conflict', status: 409, detail: 'Academic year already exists.' },
          { status: 409 },
        ),
      ),
    )

    const { user } = renderWithProviders(<AcademicYearsPage />)
    await screen.findByText('2026-27')

    await user.click(screen.getByRole('button', { name: /new year/i }))
    await user.type(await screen.findByLabelText('Label'), '2026-27')
    await user.type(screen.getByLabelText('Start date'), '2026-07-01')
    await user.type(screen.getByLabelText('End date'), '2027-06-30')
    await user.click(screen.getByRole('button', { name: 'Save' }))

    expect(await screen.findByText('Academic year already exists.')).toBeInTheDocument()
  })

  it('shows an empty state when there are no years', async () => {
    server.use(http.get(`${API_BASE_URL}/api/academic-years`, () => HttpResponse.json(pageOf([]))))
    renderWithProviders(<AcademicYearsPage />)

    expect(await screen.findByText('No academic years yet')).toBeInTheDocument()
  })
})
