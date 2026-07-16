import { describe, expect, it } from 'vitest'
import { http, HttpResponse } from 'msw'
import { renderWithProviders, screen, waitFor, server, API_BASE_URL, mockSkill } from '@/test'
import SkillsPage from './SkillsPage'

describe('SkillsPage', () => {
  it('lists the catalogue with verification state and provenance', async () => {
    renderWithProviders(<SkillsPage />)

    expect(await screen.findByText('Java')).toBeInTheDocument()
    expect(screen.getByText('Rust')).toBeInTheDocument()
    expect(screen.getByText('Verified')).toBeInTheDocument()
    expect(screen.getByText('Unverified')).toBeInTheDocument()
    expect(screen.getByText('AI')).toBeInTheDocument()
  })

  it('searches through the catalogue endpoint rather than filtering locally', async () => {
    let searchTerm: string | null = null
    server.use(
      http.get(`${API_BASE_URL}/api/skills/search`, ({ request }) => {
        searchTerm = new URL(request.url).searchParams.get('q')
        return HttpResponse.json([
          {
            id: mockSkill.id,
            name: mockSkill.name,
            category: mockSkill.category,
            parentCategory: null,
            popularityScore: 90,
            matchType: 'ALIAS',
            score: 0.9,
          },
        ])
      }),
    )

    const { user } = renderWithProviders(<SkillsPage />)
    await screen.findByText('Java')

    await user.type(screen.getByLabelText('Search skills'), 'jdk')

    // The alias 'jdk' only resolves to Java server-side — a local substring
    // filter would have found nothing.
    await waitFor(() => expect(searchTerm).toBe('jdk'))
    await waitFor(() => expect(screen.queryByText('Rust')).not.toBeInTheDocument())
    expect(screen.getByText('Java')).toBeInTheDocument()
  })

  it('only offers Verify for an unverified skill', async () => {
    renderWithProviders(<SkillsPage />)
    await screen.findByText('Rust')

    // Java is already verified, so exactly one Verify button exists (Rust's).
    expect(screen.getAllByRole('button', { name: /^verify$/i })).toHaveLength(1)
  })

  it('confirms before verifying, then applies it', async () => {
    const { user } = renderWithProviders(<SkillsPage />)
    await screen.findByText('Rust')

    await user.click(screen.getByRole('button', { name: /^verify$/i }))
    expect(await screen.findByText('Verify skill?')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Verify skill' }))
    expect(await screen.findByText('Skill verified.')).toBeInTheDocument()
  })

  it('creates a skill', async () => {
    const { user } = renderWithProviders(<SkillsPage />)
    await screen.findByText('Java')

    await user.click(screen.getByRole('button', { name: /new skill/i }))
    await user.type(await screen.findByLabelText('Name'), 'Kotlin')
    await user.click(screen.getByRole('button', { name: 'Save' }))

    expect(await screen.findByText('Skill created.')).toBeInTheDocument()
  })

  it('surfaces a duplicate-name 409 from the backend', async () => {
    server.use(
      http.post(`${API_BASE_URL}/api/skills`, () =>
        HttpResponse.json(
          { title: 'Conflict', status: 409, detail: 'Skill already exists.' },
          { status: 409 },
        ),
      ),
    )

    const { user } = renderWithProviders(<SkillsPage />)
    await screen.findByText('Java')

    await user.click(screen.getByRole('button', { name: /new skill/i }))
    await user.type(await screen.findByLabelText('Name'), 'Java')
    await user.click(screen.getByRole('button', { name: 'Save' }))

    expect(await screen.findByText('Skill already exists.')).toBeInTheDocument()
  })

  it('shows a retry affordance when the catalogue fails to load', async () => {
    server.use(
      http.get(`${API_BASE_URL}/api/skills`, () =>
        HttpResponse.json({ title: 'Server Error', status: 500 }, { status: 500 }),
      ),
    )
    renderWithProviders(<SkillsPage />)

    expect(await screen.findByText('Failed to load data')).toBeInTheDocument()
  })
})
