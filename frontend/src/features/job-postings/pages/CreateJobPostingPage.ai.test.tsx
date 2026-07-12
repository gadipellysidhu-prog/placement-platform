import { beforeEach, describe, expect, it } from 'vitest'
import { http, HttpResponse } from 'msw'
import { Route, Routes } from 'react-router-dom'
import { renderWithProviders, screen, waitFor } from '@/test'
import { server, API_BASE_URL } from '@/test'
import { useAuthStore } from '@/stores/auth.store'
import { ROUTES } from '@/constants/routes'
import CreateJobPostingPage from './CreateJobPostingPage'

/** AI import workflow on the create page (the manual path is covered by the base test). */

function stubCompanies() {
  server.use(
    http.get(`${API_BASE_URL}/api/companies`, () =>
      HttpResponse.json({
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
      }),
    ),
  )
}

function stubCreatePosting() {
  server.use(
    http.post(`${API_BASE_URL}/api/job-postings`, () =>
      HttpResponse.json({ id: 'jp-new', title: 'Backend Engineer' }, { status: 201 }),
    ),
  )
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

async function fillBaseForm(user: ReturnType<typeof renderCreate>['user']) {
  await user.click(await screen.findByRole('combobox', { name: /company/i }))
  await user.click(await screen.findByRole('option', { name: 'Acme Corp' }))
  await user.type(screen.getByLabelText(/title/i), 'Backend Engineer')
  await user.type(screen.getByLabelText(/offer limit/i), '5')
}

beforeEach(() => {
  useAuthStore.setState({
    user: { email: 'o@u.edu', role: 'ROLE_PLACEMENT_OFFICER' },
    isAuthenticated: true,
  })
})

describe('CreateJobPostingPage — AI import', () => {
  it('with import ON: creates the draft, starts the AI run, then navigates', async () => {
    stubCompanies()
    stubCreatePosting()
    let runBody: Record<string, unknown> | null = null
    server.use(
      http.post(`${API_BASE_URL}/api/job-intelligence/runs`, async ({ request }) => {
        runBody = (await request.json()) as Record<string, unknown>
        return HttpResponse.json(
          { id: 'run-1', jobPostingId: 'jp-new', status: 'PENDING', terminal: false },
          { status: 202 },
        )
      }),
    )

    const { user } = renderCreate()
    await fillBaseForm(user)

    await user.click(screen.getByRole('switch', { name: /import official job/i }))
    await user.type(screen.getByLabelText(/official job url/i), 'https://careers.acme.com/jobs/42')
    await user.click(screen.getByRole('button', { name: /create draft/i }))

    expect(await screen.findByText('Detail Screen')).toBeInTheDocument()
    await waitFor(() =>
      expect(runBody).toEqual({
        jobPostingId: 'jp-new',
        officialUrl: 'https://careers.acme.com/jobs/42',
      }),
    )
  })

  it('with import ON but an invalid URL: blocks submission with a validation error', async () => {
    stubCompanies()
    let posted = false
    server.use(
      http.post(`${API_BASE_URL}/api/job-postings`, () => {
        posted = true
        return HttpResponse.json({}, { status: 201 })
      }),
    )

    const { user } = renderCreate()
    await fillBaseForm(user)

    await user.click(screen.getByRole('switch', { name: /import official job/i }))
    await user.type(screen.getByLabelText(/official job url/i), 'not-a-url')
    await user.click(screen.getByRole('button', { name: /create draft/i }))

    expect(await screen.findByText(/valid http\(s\) url/i)).toBeInTheDocument()
    expect(posted).toBe(false)
  })

  it('with import OFF: never calls the job-intelligence API (manual mode regression)', async () => {
    stubCompanies()
    stubCreatePosting()
    let runCalled = false
    server.use(
      http.post(`${API_BASE_URL}/api/job-intelligence/runs`, () => {
        runCalled = true
        return HttpResponse.json({}, { status: 202 })
      }),
    )

    const { user } = renderCreate()
    await fillBaseForm(user)
    await user.click(screen.getByRole('button', { name: /create draft/i }))

    expect(await screen.findByText('Detail Screen')).toBeInTheDocument()
    expect(runCalled).toBe(false)
  })

  it('if starting the AI run fails, the draft still succeeds and navigation happens', async () => {
    stubCompanies()
    stubCreatePosting()
    server.use(
      http.post(`${API_BASE_URL}/api/job-intelligence/runs`, () =>
        HttpResponse.json(
          { title: 'Service Unavailable', status: 503, detail: 'AI disabled' },
          { status: 503 },
        ),
      ),
    )

    const { user } = renderCreate()
    await fillBaseForm(user)

    await user.click(screen.getByRole('switch', { name: /import official job/i }))
    await user.type(screen.getByLabelText(/official job url/i), 'https://careers.acme.com/x')
    await user.click(screen.getByRole('button', { name: /create draft/i }))

    // Progressive enhancement: AI failure never blocks the manual workflow.
    expect(await screen.findByText('Detail Screen')).toBeInTheDocument()
  })
})
