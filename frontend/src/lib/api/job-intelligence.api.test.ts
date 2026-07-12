import { describe, expect, it } from 'vitest'
import { http, HttpResponse } from 'msw'
import { server, API_BASE_URL } from '@/test'
import { jobIntelligenceApi } from './job-intelligence.api'
import type { JobIntelligenceRun } from './job-intelligence.api'

/** Contract tests: exact method/path/payload for every job-intelligence endpoint. */

function run(overrides: Partial<JobIntelligenceRun> = {}): JobIntelligenceRun {
  return {
    id: 'run-1',
    jobPostingId: 'jp-1',
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

describe('jobIntelligenceApi', () => {
  it('startRun() POSTs the posting id and URL to /api/job-intelligence/runs', async () => {
    let seen: { method: string; url: string; body: unknown } | null = null
    server.use(
      http.post(`${API_BASE_URL}/api/job-intelligence/runs`, async ({ request }) => {
        seen = { method: request.method, url: request.url, body: await request.json() }
        return HttpResponse.json(run(), { status: 202 })
      }),
    )

    const result = await jobIntelligenceApi.startRun({
      jobPostingId: 'jp-1',
      officialUrl: 'https://careers.example.com/jobs/1',
    })

    expect(seen!.method).toBe('POST')
    expect(seen!.url).toBe(`${API_BASE_URL}/api/job-intelligence/runs`)
    expect(seen!.body).toEqual({
      jobPostingId: 'jp-1',
      officialUrl: 'https://careers.example.com/jobs/1',
    })
    expect(result.status).toBe('PENDING')
  })

  it('getRun() GETs /api/job-intelligence/runs/{id}', async () => {
    server.use(
      http.get(`${API_BASE_URL}/api/job-intelligence/runs/run-1`, () =>
        HttpResponse.json(run({ status: 'EXTRACTING' })),
      ),
    )

    expect((await jobIntelligenceApi.getRun('run-1')).status).toBe('EXTRACTING')
  })

  it('latestForPosting() GETs /api/job-intelligence/postings/{postingId}/latest', async () => {
    let url: string | null = null
    server.use(
      http.get(`${API_BASE_URL}/api/job-intelligence/postings/jp-1/latest`, ({ request }) => {
        url = request.url
        return HttpResponse.json(run({ status: 'COMPLETED', terminal: true }))
      }),
    )

    const result = await jobIntelligenceApi.latestForPosting('jp-1')

    expect(url).toBe(`${API_BASE_URL}/api/job-intelligence/postings/jp-1/latest`)
    expect(result.terminal).toBe(true)
  })

  it('retry() POSTs to /api/job-intelligence/runs/{id}/retry', async () => {
    let method: string | null = null
    server.use(
      http.post(`${API_BASE_URL}/api/job-intelligence/runs/run-1/retry`, ({ request }) => {
        method = request.method
        return HttpResponse.json(run({ retryCount: 1 }), { status: 202 })
      }),
    )

    expect((await jobIntelligenceApi.retry('run-1')).retryCount).toBe(1)
    expect(method).toBe('POST')
  })
})
