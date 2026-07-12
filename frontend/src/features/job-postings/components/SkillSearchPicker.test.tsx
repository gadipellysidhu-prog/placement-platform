import { describe, expect, it, vi } from 'vitest'
import { http, HttpResponse } from 'msw'
import { renderWithProviders, screen, waitFor } from '@/test'
import { server, API_BASE_URL } from '@/test'
import type { SkillSearchResult } from '@/lib/api'
import { SkillSearchPicker } from './SkillSearchPicker'

function result(overrides: Partial<SkillSearchResult>): SkillSearchResult {
  return {
    id: 's-react',
    name: 'React',
    category: 'Web Frontend',
    parentCategory: 'Software',
    popularityScore: 95,
    matchType: 'EXACT',
    score: 1,
    ...overrides,
  }
}

function stubSearch(results: SkillSearchResult[], seenQueries: string[] = []) {
  server.use(
    http.get(`${API_BASE_URL}/api/skills/search`, ({ request }) => {
      seenQueries.push(new URL(request.url).searchParams.get('q') ?? '')
      return HttpResponse.json(results)
    }),
  )
}

describe('SkillSearchPicker', () => {
  it('finds a skill by alias, groups by category, and adds it on click', async () => {
    const seenQueries: string[] = []
    stubSearch(
      [
        result({ id: 's-react', name: 'React', matchType: 'ALIAS', score: 0.95 }),
        result({ id: 's-redux', name: 'Redux', matchType: 'PARTIAL', score: 0.7 }),
      ],
      seenQueries,
    )
    const onAdd = vi.fn()

    const { user } = renderWithProviders(
      <SkillSearchPicker attachedIds={new Set()} onAdd={onAdd} />,
    )

    await user.type(screen.getByRole('combobox', { name: /search skills/i }), 'reactjs')

    const option = await screen.findByRole('option', { name: /react.*alias/i })
    expect(screen.getByText('Web Frontend')).toBeInTheDocument() // category group header
    expect(screen.getByRole('option', { name: /redux/i })).toBeInTheDocument()
    await user.click(option)

    expect(onAdd).toHaveBeenCalledWith('s-react')
    await waitFor(() => expect(seenQueries).toContain('reactjs'))
  })

  it('supports keyboard selection: ArrowDown + Enter adds the highlighted skill', async () => {
    stubSearch([
      result({ id: 's-ml', name: 'Machine Learning', matchType: 'ALIAS' }),
      result({ id: 's-mlops', name: 'MLOps', matchType: 'PARTIAL' }),
    ])
    const onAdd = vi.fn()

    const { user } = renderWithProviders(
      <SkillSearchPicker attachedIds={new Set()} onAdd={onAdd} />,
    )

    const input = screen.getByRole('combobox', { name: /search skills/i })
    await user.type(input, 'ml')
    await screen.findByRole('option', { name: /machine learning/i })
    await user.keyboard('{ArrowDown}{Enter}')

    expect(onAdd).toHaveBeenCalledWith('s-mlops')
  })

  it('hides skills that are already attached to the posting', async () => {
    stubSearch([
      result({ id: 's-java', name: 'Java' }),
      result({ id: 's-javascript', name: 'JavaScript', matchType: 'PARTIAL' }),
    ])

    const { user } = renderWithProviders(
      <SkillSearchPicker attachedIds={new Set(['s-java'])} onAdd={vi.fn()} />,
    )

    await user.type(screen.getByRole('combobox', { name: /search skills/i }), 'java')

    expect(await screen.findByRole('option', { name: /javascript/i })).toBeInTheDocument()
    expect(screen.queryByRole('option', { name: /^java\b/i })).not.toBeInTheDocument()
  })

  it('shows an empty state for a query with no matches', async () => {
    stubSearch([])

    const { user } = renderWithProviders(
      <SkillSearchPicker attachedIds={new Set()} onAdd={vi.fn()} />,
    )

    await user.type(screen.getByRole('combobox', { name: /search skills/i }), 'zzz')

    expect(await screen.findByText(/no matching skills/i)).toBeInTheDocument()
  })
})
