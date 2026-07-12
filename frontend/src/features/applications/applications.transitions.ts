import type { ApplicationStatus } from '@/lib/api'

/**
 * A status a placement officer may set through `PUT /api/applications/{id}/status`.
 * Excludes APPLIED (the initial state, never a transition target) and WITHDRAWN
 * (reachable only through the student withdraw endpoint). Assignable to
 * `UpdateApplicationStatusRequest['status']`.
 */
export type OfficerSettableStatus = Exclude<ApplicationStatus, 'APPLIED' | 'WITHDRAWN'>

/**
 * Legal application-status transitions, mirrored verbatim from the backend
 * `JobApplicationService.validateStatusTransition` (the source of truth):
 *
 *   APPLIED     -> SHORTLISTED, REJECTED, WITHDRAWN
 *   SHORTLISTED -> INTERVIEWED, REJECTED, WITHDRAWN
 *   INTERVIEWED -> OFFERED, REJECTED
 *   OFFERED / REJECTED / WITHDRAWN -> (terminal, no transitions)
 *
 * WITHDRAWN is only ever reached through the dedicated student withdraw endpoint
 * (`POST /api/applications/{id}/withdraw`), never through the officer status-update
 * endpoint (`PUT /api/applications/{id}/status`). {@link OFFICER_TRANSITIONS} is
 * therefore the backend map with WITHDRAWN removed — exactly the set of targets a
 * placement officer may set — so the transition UI never offers an illegal option.
 */
export const OFFICER_TRANSITIONS: Record<ApplicationStatus, OfficerSettableStatus[]> = {
  APPLIED: ['SHORTLISTED', 'REJECTED'],
  SHORTLISTED: ['INTERVIEWED', 'REJECTED'],
  INTERVIEWED: ['OFFERED', 'REJECTED'],
  OFFERED: [],
  REJECTED: [],
  WITHDRAWN: [],
}

/** Terminal states, mirrored from the backend: no further transition is possible. */
export const TERMINAL_STATUSES: ReadonlySet<ApplicationStatus> = new Set([
  'OFFERED',
  'REJECTED',
  'WITHDRAWN',
])

/** Officer-legal target statuses for a given current status (never empty for non-terminal). */
export function officerTransitionsFor(status: ApplicationStatus): OfficerSettableStatus[] {
  return OFFICER_TRANSITIONS[status] ?? []
}

/**
 * Whether the owning student may withdraw an application in this status.
 * Mirrors `JobApplicationService.withdraw`, which rejects OFFERED, REJECTED and
 * WITHDRAWN with 422 — i.e. withdrawal is allowed only from a non-terminal state.
 */
export function canWithdraw(status: ApplicationStatus): boolean {
  return !TERMINAL_STATUSES.has(status)
}
