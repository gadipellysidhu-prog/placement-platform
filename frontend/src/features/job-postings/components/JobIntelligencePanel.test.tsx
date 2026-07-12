import { beforeEach, describe, expect, it } from 'vitest'
import { http, HttpResponse } from 'msw'
import { renderWithProviders, screen, waitFor } from '@/test'
import { server, API_BASE_URL } from '@/test'
import { useAuthStore } from '@/stores/auth.store'
import type { JobIntelligenceRun } from '@/lib/api'
import JobIntelligencePanel from './JobIntelligencePanel'

const POSTING_ID = 'jp-1'

function run(overrides: Partial<JobIntelligenceRun> = {}): JobIntelligenceRun {
  return {
    id: 'run-1',
    jobPostingId: POSTING_ID,
    officialUrl: 'https://careers.example.com/jobs/1',
    status: 'PENDING',
    terminal: false,
    provider: null,
    model: null,
    confidence: null,
    skillsExtracted: 0,
    skillsCreated: 0,
    skillsTagged: 0,
    predictedBranches: [],
    warnings: [],
    errorMessage: null,
    retryCount: 0,
    startedAt: null,
    completedAt: null,
    durationMs: null,
    createdAt: '2026-07-12T10:00:00Z',
    ...overrides,
  }
}

beforeEach(() => {
  useAuthStore.setState({
    user: { email: 'o@u.edu', role: 'ROLE_PLACEMENT_OFFICER' },
    isAuthenticated: true,
  })
})

describe('JobIntelligencePanel', () => {
  it('renders nothing when the posting has never been analyzed (404)', async () => {
    server.use(
      http.get(`${API_BASE_URL}/api/job-intelligence/postings/${POSTING_ID}/latest`, () =>
        HttpResponse.json(
          { title: 'Not Found', status: 404, detail: 'No AI analysis exists' },
          { status: 404 },
        ),
      ),
    )

    renderWithProviders(<JobIntelligencePanel postingId={POSTING_ID} />)

    // The 404 must resolve to "no panel", never an error state or a retry loop.
    await waitFor(() => expect(screen.queryByText(/ai analysis/i)).not.toBeInTheDocument())
    await new Promise((resolve) => setTimeout(resolve, 300))
    expect(screen.queryByText(/ai analysis/i)).not.toBeInTheDocument()
  })

  it('shows live stage progress while the pipeline runs, then the summary on completion', async () => {
    let call = 0
    server.use(
      http.get(`${API_BASE_URL}/api/job-intelligence/postings/${POSTING_ID}/latest`, () => {
        call += 1
        if (call === 1) {
          return HttpResponse.json(run({ status: 'EXTRACTING' }))
        }
        return HttpResponse.json(
          run({
            status: 'COMPLETED',
            terminal: true,
            provider: 'openai-compatible',
            confidence: 92,
            skillsExtracted: 12,
            skillsCreated: 2,
            skillsTagged: 10,
            predictedBranches: ['Computer Science', 'ECE'],
            durationMs: 8400,
          }),
        )
      }),
    )

    renderWithProviders(<JobIntelligencePanel postingId={POSTING_ID} />)

    // Running: the active stage is announced.
    expect(await screen.findByText(/running ai extraction/i)).toBeInTheDocument()

    // Poller flips to COMPLETED → summary card appears without any reload.
    expect(await screen.findByText('Completed', {}, { timeout: 5000 })).toBeInTheDocument()
    expect(screen.getByText('12')).toBeInTheDocument() // skills extracted
    expect(screen.getByText('10')).toBeInTheDocument() // skills attached
    expect(screen.getByText('92%')).toBeInTheDocument()
    expect(screen.getByText('Computer Science')).toBeInTheDocument()
  })

  it('failed run shows the error and retries via the retry endpoint', async () => {
    let retried = false
    server.use(
      http.get(`${API_BASE_URL}/api/job-intelligence/postings/${POSTING_ID}/latest`, () =>
        HttpResponse.json(
          run({
            status: 'FAILED',
            terminal: true,
            errorMessage: 'Job page not found (HTTP 404)',
          }),
        ),
      ),
      http.post(`${API_BASE_URL}/api/job-intelligence/runs/run-1/retry`, () => {
        retried = true
        return HttpResponse.json(run({ status: 'PENDING', retryCount: 1 }), { status: 202 })
      }),
    )

    const { user } = renderWithProviders(<JobIntelligencePanel postingId={POSTING_ID} />)

    expect(await screen.findByText(/job page not found/i)).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: /retry ai analysis/i }))

    await waitFor(() => expect(retried).toBe(true))
  })

  it('exposes warnings in the collapsible activity section', async () => {
    server.use(
      http.get(`${API_BASE_URL}/api/job-intelligence/postings/${POSTING_ID}/latest`, () =>
        HttpResponse.json(
          run({
            status: 'COMPLETED',
            terminal: true,
            warnings: ["Skill 'Quantum Weaving' failed: not found"],
          }),
        ),
      ),
    )

    const { user } = renderWithProviders(<JobIntelligencePanel postingId={POSTING_ID} />)

    await user.click(await screen.findByRole('button', { name: /activity details/i }))

    expect(screen.getByText(/quantum weaving/i)).toBeInTheDocument()
  })
})
