import type { ApplicationStatus, JobApplicationResponse } from '@/lib/api'

export interface TimelineEvent {
  status: ApplicationStatus
  at: string
}

/**
 * Derives the application timeline from the fields the backend actually exposes.
 *
 * The backend persists no per-transition status history via any API (the
 * `application_status_history` table is not surfaced), so the only two events we can
 * report faithfully are:
 *   1. APPLIED, at `appliedAt` (immutable, set on creation), and
 *   2. the current status, at `updatedAt`, when it has moved past APPLIED.
 *
 * We deliberately do not fabricate intermediate transitions we cannot observe.
 */
export function buildApplicationTimeline(application: JobApplicationResponse): TimelineEvent[] {
  const events: TimelineEvent[] = [{ status: 'APPLIED', at: application.appliedAt }]
  if (application.status !== 'APPLIED') {
    events.push({ status: application.status, at: application.updatedAt })
  }
  return events
}
