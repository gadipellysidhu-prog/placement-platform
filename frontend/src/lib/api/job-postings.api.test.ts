import { describe, expect, it } from 'vitest'
import { http, HttpResponse } from 'msw'
import { server, API_BASE_URL } from '@/test'
import { jobPostingsApi } from './job-postings.api'
import type { Page } from '@/types'
import type { JobPostingResponse } from './job-postings.api'

/**
 * Contract tests: every method of the API client is exercised against MSW handlers that
 * assert the exact HTTP method, path, query params, and request body the backend
 * JobPostingController expects. This makes the client the verified frontend↔backend seam.
 */

function summary(overrides: Partial<JobPostingResponse> = {}): JobPostingResponse {
  return {
    id: '11111111-1111-1111-1111-111111111111',
    companyId: '22222222-2222-2222-2222-222222222222',
    companyName: 'Acme Corp',
    title: 'Backend Engineer',
    description: 'Build things',
    ctcMin: 8,
    ctcMax: 12,
    status: 'DRAFT',
    applicationDeadline: '2026-08-01',
    offerLimit: 5,
    requiredSkills: null,
    eligibleBranches: null,
    createdAt: '2026-07-01T10:00:00Z',
    updatedAt: '2026-07-01T10:00:00Z',
    ...overrides,
  }
}

function emptyPage(content: JobPostingResponse[]): Page<JobPostingResponse> {
  return {
    content,
    pageable: { pageNumber: 0, pageSize: 20, sort: { sorted: false } },
    totalElements: content.length,
    totalPages: 1,
    last: true,
    first: true,
    numberOfElements: content.length,
    size: 20,
    number: 0,
    empty: content.length === 0,
  }
}

describe('jobPostingsApi', () => {
  it('create() POSTs to /api/job-postings with the create payload', async () => {
    let seen: { method: string; url: string; body: unknown } | null = null
    server.use(
      http.post(`${API_BASE_URL}/api/job-postings`, async ({ request }) => {
        seen = { method: request.method, url: request.url, body: await request.json() }
        return HttpResponse.json(summary(), { status: 201 })
      }),
    )

    const result = await jobPostingsApi.create({
      companyId: '22222222-2222-2222-2222-222222222222',
      title: 'Backend Engineer',
      offerLimit: 5,
    })

    expect(seen!.method).toBe('POST')
    expect(seen!.url).toBe(`${API_BASE_URL}/api/job-postings`)
    expect(seen!.body).toEqual({
      companyId: '22222222-2222-2222-2222-222222222222',
      title: 'Backend Engineer',
      offerLimit: 5,
    })
    expect(result.status).toBe('DRAFT')
  })

  it('list() GETs /api/job-postings passing page, size and title params', async () => {
    let url: URL | null = null
    server.use(
      http.get(`${API_BASE_URL}/api/job-postings`, ({ request }) => {
        url = new URL(request.url)
        return HttpResponse.json(emptyPage([summary({ status: 'OPEN' })]))
      }),
    )

    const page = await jobPostingsApi.list({ page: 2, size: 10, title: 'engineer' })

    expect(url!.pathname).toBe('/api/job-postings')
    expect(url!.searchParams.get('page')).toBe('2')
    expect(url!.searchParams.get('size')).toBe('10')
    expect(url!.searchParams.get('title')).toBe('engineer')
    expect(page.content[0].status).toBe('OPEN')
  })

  it('listManage() GETs /api/job-postings/manage passing the status filter', async () => {
    let url: URL | null = null
    server.use(
      http.get(`${API_BASE_URL}/api/job-postings/manage`, ({ request }) => {
        url = new URL(request.url)
        return HttpResponse.json(emptyPage([summary(), summary({ status: 'OPEN' })]))
      }),
    )

    const page = await jobPostingsApi.listManage({ page: 0, size: 20, status: 'DRAFT' })

    expect(url!.pathname).toBe('/api/job-postings/manage')
    expect(url!.searchParams.get('status')).toBe('DRAFT')
    expect(page.content).toHaveLength(2)
  })

  it('getById() GETs the detail resource including skills and branches', async () => {
    server.use(
      http.get(`${API_BASE_URL}/api/job-postings/abc`, () =>
        HttpResponse.json(
          summary({
            id: 'abc',
            requiredSkills: [{ id: 's1', name: 'Java' }],
            eligibleBranches: [{ id: 'b1', name: 'CSE' }],
          }),
        ),
      ),
    )

    const result = await jobPostingsApi.getById('abc')

    expect(result.requiredSkills).toEqual([{ id: 's1', name: 'Java' }])
    expect(result.eligibleBranches).toEqual([{ id: 'b1', name: 'CSE' }])
  })

  it('update() PUTs to /api/job-postings/{id} WITHOUT a companyId field', async () => {
    let seen: { method: string; url: string; body: Record<string, unknown> } | null = null
    server.use(
      http.put(`${API_BASE_URL}/api/job-postings/xyz`, async ({ request }) => {
        seen = {
          method: request.method,
          url: request.url,
          body: (await request.json()) as Record<string, unknown>,
        }
        return HttpResponse.json(summary({ id: 'xyz', title: 'Updated' }))
      }),
    )

    await jobPostingsApi.update('xyz', { title: 'Updated', offerLimit: 3 })

    expect(seen!.method).toBe('PUT')
    expect(seen!.url).toBe(`${API_BASE_URL}/api/job-postings/xyz`)
    expect(seen!.body).toEqual({ title: 'Updated', offerLimit: 3 })
    expect(seen!.body).not.toHaveProperty('companyId')
  })

  it.each([
    ['open', 'open'],
    ['close', 'close'],
    ['cancel', 'cancel'],
  ] as const)('%s() POSTs to /api/job-postings/{id}/%s', async (method, segment) => {
    let seen: { method: string; url: string } | null = null
    server.use(
      http.post(`${API_BASE_URL}/api/job-postings/pid/${segment}`, ({ request }) => {
        seen = { method: request.method, url: request.url }
        return HttpResponse.json(summary({ id: 'pid' }))
      }),
    )

    await jobPostingsApi[method]('pid')

    expect(seen!.method).toBe('POST')
    expect(seen!.url).toBe(`${API_BASE_URL}/api/job-postings/pid/${segment}`)
  })

  it('addSkill()/removeSkill() hit the nested skills path with POST/DELETE', async () => {
    const calls: { method: string; url: string }[] = []
    server.use(
      http.post(`${API_BASE_URL}/api/job-postings/pid/skills/sid`, ({ request }) => {
        calls.push({ method: request.method, url: request.url })
        return HttpResponse.json(summary({ requiredSkills: [{ id: 'sid', name: 'Go' }] }))
      }),
      http.delete(`${API_BASE_URL}/api/job-postings/pid/skills/sid`, ({ request }) => {
        calls.push({ method: request.method, url: request.url })
        return HttpResponse.json(summary({ requiredSkills: [] }))
      }),
    )

    const added = await jobPostingsApi.addSkill('pid', 'sid')
    const removed = await jobPostingsApi.removeSkill('pid', 'sid')

    expect(calls[0]).toEqual({
      method: 'POST',
      url: `${API_BASE_URL}/api/job-postings/pid/skills/sid`,
    })
    expect(calls[1]).toEqual({
      method: 'DELETE',
      url: `${API_BASE_URL}/api/job-postings/pid/skills/sid`,
    })
    expect(added.requiredSkills).toEqual([{ id: 'sid', name: 'Go' }])
    expect(removed.requiredSkills).toEqual([])
  })

  it('addBranch()/removeBranch() hit the nested branches path with POST/DELETE', async () => {
    const calls: { method: string; url: string }[] = []
    server.use(
      http.post(`${API_BASE_URL}/api/job-postings/pid/branches/bid`, ({ request }) => {
        calls.push({ method: request.method, url: request.url })
        return HttpResponse.json(summary({ eligibleBranches: [{ id: 'bid', name: 'ECE' }] }))
      }),
      http.delete(`${API_BASE_URL}/api/job-postings/pid/branches/bid`, ({ request }) => {
        calls.push({ method: request.method, url: request.url })
        return HttpResponse.json(summary({ eligibleBranches: [] }))
      }),
    )

    await jobPostingsApi.addBranch('pid', 'bid')
    await jobPostingsApi.removeBranch('pid', 'bid')

    expect(calls[0].method).toBe('POST')
    expect(calls[0].url).toBe(`${API_BASE_URL}/api/job-postings/pid/branches/bid`)
    expect(calls[1].method).toBe('DELETE')
    expect(calls[1].url).toBe(`${API_BASE_URL}/api/job-postings/pid/branches/bid`)
  })
})
