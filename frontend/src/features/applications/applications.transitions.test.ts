import { describe, expect, it } from 'vitest'
import type { ApplicationStatus } from '@/lib/api'
import {
  OFFICER_TRANSITIONS,
  TERMINAL_STATUSES,
  canWithdraw,
  officerTransitionsFor,
} from './applications.transitions'

const ALL_STATUSES: ApplicationStatus[] = [
  'APPLIED',
  'SHORTLISTED',
  'INTERVIEWED',
  'OFFERED',
  'REJECTED',
  'WITHDRAWN',
]

describe('OFFICER_TRANSITIONS — mirrors backend validateStatusTransition (WITHDRAWN excluded)', () => {
  it('offers the exact officer-legal targets for each non-terminal status', () => {
    expect(officerTransitionsFor('APPLIED')).toEqual(['SHORTLISTED', 'REJECTED'])
    expect(officerTransitionsFor('SHORTLISTED')).toEqual(['INTERVIEWED', 'REJECTED'])
    expect(officerTransitionsFor('INTERVIEWED')).toEqual(['OFFERED', 'REJECTED'])
  })

  it('offers no transitions from any terminal status', () => {
    expect(officerTransitionsFor('OFFERED')).toEqual([])
    expect(officerTransitionsFor('REJECTED')).toEqual([])
    expect(officerTransitionsFor('WITHDRAWN')).toEqual([])
  })

  it('never offers WITHDRAWN as an officer transition (that is the withdraw endpoint)', () => {
    for (const status of ALL_STATUSES) {
      expect(OFFICER_TRANSITIONS[status]).not.toContain('WITHDRAWN')
    }
  })

  it('never offers an illegal skip transition (e.g. APPLIED -> INTERVIEWED/OFFERED)', () => {
    expect(officerTransitionsFor('APPLIED')).not.toContain('INTERVIEWED')
    expect(officerTransitionsFor('APPLIED')).not.toContain('OFFERED')
    expect(officerTransitionsFor('SHORTLISTED')).not.toContain('OFFERED')
  })
})

describe('canWithdraw — mirrors backend JobApplicationService.withdraw', () => {
  it('allows withdrawal only from non-terminal states', () => {
    expect(canWithdraw('APPLIED')).toBe(true)
    expect(canWithdraw('SHORTLISTED')).toBe(true)
    expect(canWithdraw('INTERVIEWED')).toBe(true)
  })

  it('rejects withdrawal from terminal states (backend returns 422)', () => {
    expect(canWithdraw('OFFERED')).toBe(false)
    expect(canWithdraw('REJECTED')).toBe(false)
    expect(canWithdraw('WITHDRAWN')).toBe(false)
    expect([...TERMINAL_STATUSES].sort()).toEqual(['OFFERED', 'REJECTED', 'WITHDRAWN'])
  })
})
