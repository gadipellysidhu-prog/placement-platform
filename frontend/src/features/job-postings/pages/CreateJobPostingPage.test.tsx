import { beforeEach, describe, expect, it } from 'vitest'
import { http, HttpResponse } from 'msw'
import { Route, Routes } from 'react-router-dom'
import { renderWithProviders, screen, waitFor } from '@/test'
import { server, API_BASE_URL } from '@/test'
import { useAuthStore } from '@/stores/auth.store'
import { ROUTES } from '@/constants/routes'
import CreateJobPostingPage from './CreateJobPostingPage'

function companiesPage() {
  return {
    content: [
      {
        id: 'co-1',
        name: 'Acme Corp',
        website: null,
        industry: null,
        description: null,
        logoUrl: null,
        status: 'ACTIVE',
        createdAt: '',
        updatedAt: '',
      },
    ],
    pageable: { pageNumber: 0, pageSize: 200, sort: { sorted: false } },
    totalElements: 1,
    totalPages: 1,
    last: true,
    first: true,
    numberOfElements: 1,
    size: 200,
    number: 0,
    empty: false,
  }
}

function stubCompanies() {
  server.use(http.get(`${API_BASE_URL}/api/companies`, () => HttpResponse.json(companiesPage())))
}

function renderCreate() {
  return renderWithProviders(
    <Routes>
      <Route path={ROUTES.OFFICER.CREATE_JOB_POSTING} element={<CreateJobPostingPage />} />
      <Route path={`${ROUTES.OFFICER.JOB_POSTINGS}/:id`} element={<div>Detail Screen</div>} />
    </Routes>,
    { initialEntries: [ROUTES.OFFICER.CREATE_JOB_POSTING] },
  )
}

beforeEach(() => {
  useAuthStore.setState({
    user: { email: 'o@u.edu', role: 'ROLE_PLACEMENT_OFFICER' },
    isAuthenticated: true,
  })
})

describe('CreateJobPostingPage', () => {
  it('blocks submission and shows validation errors for an empty form', async () => {
    stubCompanies()
    let posted = false
    server.use(
      http.post(`${API_BASE_URL}/api/job-postings`, () => {
        posted = true
        return HttpResponse.json({}, { status: 201 })
      }),
    )

    const { user } = renderCreate()
    await user.click(await screen.findByRole('button', { name: /create draft/i }))

    expect(await screen.findByText(/company is required/i)).toBeInTheDocument()
    expect(screen.getByText(/title is required/i)).toBeInTheDocument()
    expect(screen.getByText(/offer limit is required/i)).toBeInTheDocument()
    expect(posted).toBe(false)
  })

  it('submits a valid posting and navigates to its detail page', async () => {
    stubCompanies()
    let body: Record<string, unknown> | null = null
    server.use(
      http.post(`${API_BASE_URL}/api/job-postings`, async ({ request }) => {
        body = (await request.json()) as Record<string, unknown>
        return HttpResponse.json({ id: 'jp-new', title: 'Backend Engineer' }, { status: 201 })
      }),
    )

    const { user } = renderCreate()

    await user.click(await screen.findByRole('combobox', { name: /company/i }))
    await user.click(await screen.findByRole('option', { name: 'Acme Corp' }))
    await user.type(screen.getByLabelText(/title/i), 'Backend Engineer')
    await user.type(screen.getByLabelText(/offer limit/i), '5')

    await user.click(screen.getByRole('button', { name: /create draft/i }))

    expect(await screen.findByText('Detail Screen')).toBeInTheDocument()
    expect(body).toEqual({
      companyId: 'co-1',
      title: 'Backend Engineer',
      offerLimit: 5,
    })
  })

  it('renders a backend ProblemDetail error as a toast without leaving the form', async () => {
    stubCompanies()
    server.use(
      http.post(`${API_BASE_URL}/api/job-postings`, () =>
        HttpResponse.json(
          { title: 'Unprocessable Entity', status: 422, detail: 'Company is blacklisted' },
          { status: 422 },
        ),
      ),
    )

    const { user } = renderCreate()

    await user.click(await screen.findByRole('combobox', { name: /company/i }))
    await user.click(await screen.findByRole('option', { name: 'Acme Corp' }))
    await user.type(screen.getByLabelText(/title/i), 'Backend Engineer')
    await user.type(screen.getByLabelText(/offer limit/i), '5')
    await user.click(screen.getByRole('button', { name: /create draft/i }))

    expect(await screen.findByText(/company is blacklisted/i)).toBeInTheDocument()
    // Still on the create form (navigation did not occur).
    await waitFor(() =>
      expect(screen.getByRole('button', { name: /create draft/i })).toBeInTheDocument(),
    )
  })
})
