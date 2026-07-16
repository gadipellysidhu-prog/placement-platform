import { describe, expect, it } from 'vitest'
import { http, HttpResponse } from 'msw'
import { renderWithProviders, screen, waitFor, server, API_BASE_URL, mockBranch } from '@/test'
import BranchesPage from './BranchesPage'

describe('BranchesPage', () => {
  it('lists the full catalogue including deactivated branches', async () => {
    renderWithProviders(<BranchesPage />)

    expect(await screen.findByText('Computer Science')).toBeInTheDocument()
    // Without activeOnly=false this row would be invisible and unreactivatable.
    expect(screen.getByText('Retired Branch')).toBeInTheDocument()
    expect(screen.getByText('Inactive')).toBeInTheDocument()
  })

  it('asks the backend for inactive branches too', async () => {
    let activeOnly: string | null = null
    server.use(
      http.get(`${API_BASE_URL}/api/branches`, ({ request }) => {
        activeOnly = new URL(request.url).searchParams.get('activeOnly')
        return HttpResponse.json([mockBranch])
      }),
    )
    renderWithProviders(<BranchesPage />)
    await screen.findByText('Computer Science')

    expect(activeOnly).toBe('false')
  })

  it('offers Activate for a deactivated branch and applies it', async () => {
    const { user } = renderWithProviders(<BranchesPage />)
    await screen.findByText('Retired Branch')

    await user.click(screen.getByRole('button', { name: 'Activate' }))
    expect(await screen.findByText('Activate branch?')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Activate branch' }))
    expect(await screen.findByText('Branch activated.')).toBeInTheDocument()
  })

  it('confirms before deactivating an active branch', async () => {
    const { user } = renderWithProviders(<BranchesPage />)
    await screen.findByText('Computer Science')

    await user.click(screen.getByRole('button', { name: 'Deactivate' }))
    expect(await screen.findByText('Deactivate branch?')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Deactivate branch' }))
    expect(await screen.findByText('Branch deactivated.')).toBeInTheDocument()
  })

  it('creates a branch', async () => {
    const { user } = renderWithProviders(<BranchesPage />)
    await screen.findByText('Computer Science')

    await user.click(screen.getByRole('button', { name: /new branch/i }))
    await user.type(await screen.findByLabelText('Name'), 'Mechanical')
    await user.click(screen.getByRole('button', { name: 'Save' }))

    expect(await screen.findByText('Branch created.')).toBeInTheDocument()
  })

  it('surfaces a duplicate-name 409 from the backend', async () => {
    server.use(
      http.post(`${API_BASE_URL}/api/branches`, () =>
        HttpResponse.json(
          { title: 'Conflict', status: 409, detail: 'Branch name already exists.' },
          { status: 409 },
        ),
      ),
    )

    const { user } = renderWithProviders(<BranchesPage />)
    await screen.findByText('Computer Science')

    await user.click(screen.getByRole('button', { name: /new branch/i }))
    await user.type(await screen.findByLabelText('Name'), 'Computer Science')
    await user.click(screen.getByRole('button', { name: 'Save' }))

    expect(await screen.findByText('Branch name already exists.')).toBeInTheDocument()
  })

  it('filters the catalogue by name', async () => {
    const { user } = renderWithProviders(<BranchesPage />)
    await screen.findByText('Computer Science')

    await user.type(screen.getByLabelText('Search branches'), 'Retired')

    await waitFor(() => expect(screen.queryByText('Computer Science')).not.toBeInTheDocument())
    expect(screen.getByText('Retired Branch')).toBeInTheDocument()
  })

  it('shows a retry affordance when the catalogue fails to load', async () => {
    server.use(
      http.get(`${API_BASE_URL}/api/branches`, () =>
        HttpResponse.json({ title: 'Server Error', status: 500 }, { status: 500 }),
      ),
    )
    renderWithProviders(<BranchesPage />)

    expect(await screen.findByText('Failed to load data')).toBeInTheDocument()
  })
})
