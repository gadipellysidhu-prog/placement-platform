import { describe, expect, it } from 'vitest'
import { renderWithProviders, screen, within } from '@/test'
import type { JobApplicationResponse } from '@/lib/api'
import { ApplicationTimeline } from './ApplicationTimeline'
import { buildApplicationTimeline } from '../applications.timeline'

function application(overrides: Partial<JobApplicationResponse> = {}): JobApplicationResponse {
  return {
    id: 'app-1',
    studentId: 'stu-1',
    studentRollNumber: 'CS-001',
    jobPostingId: 'jp-1',
    jobPostingTitle: 'Backend Engineer',
    companyId: 'co-1',
    companyName: 'Acme Corp',
    status: 'APPLIED',
    appliedAt: '2026-07-05T10:00:00Z',
    createdAt: '2026-07-05T10:00:00Z',
    updatedAt: '2026-07-08T12:00:00Z',
    ...overrides,
  }
}

describe('buildApplicationTimeline', () => {
  it('shows only the APPLIED event for a freshly applied application', () => {
    const events = buildApplicationTimeline(application({ status: 'APPLIED' }))
    expect(events).toEqual([{ status: 'APPLIED', at: '2026-07-05T10:00:00Z' }])
  })

  it('adds the current status at updatedAt once it moves past APPLIED', () => {
    const events = buildApplicationTimeline(application({ status: 'SHORTLISTED' }))
    expect(events).toEqual([
      { status: 'APPLIED', at: '2026-07-05T10:00:00Z' },
      { status: 'SHORTLISTED', at: '2026-07-08T12:00:00Z' },
    ])
  })

  it('does not fabricate intermediate transitions it cannot observe', () => {
    // Backend exposes no history, so an OFFERED application still yields exactly two events.
    const events = buildApplicationTimeline(application({ status: 'OFFERED' }))
    expect(events).toHaveLength(2)
    expect(events.map((e) => e.status)).toEqual(['APPLIED', 'OFFERED'])
  })
})

describe('ApplicationTimeline', () => {
  it('renders each event as a status badge inside the timeline list', () => {
    renderWithProviders(
      <ApplicationTimeline application={application({ status: 'INTERVIEWED' })} />,
    )
    const timeline = screen.getByRole('list', { name: /application timeline/i })
    expect(within(timeline).getByText('Applied')).toBeInTheDocument()
    expect(within(timeline).getByText('Interviewed')).toBeInTheDocument()
  })
})
