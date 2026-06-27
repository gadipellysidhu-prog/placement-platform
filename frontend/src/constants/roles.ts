import type { Role } from '@/types/auth'

export const ROLES = {
  STUDENT: 'ROLE_STUDENT' as const satisfies Role,
  PLACEMENT_OFFICER: 'ROLE_PLACEMENT_OFFICER' as const satisfies Role,
  ADMIN: 'ROLE_ADMIN' as const satisfies Role,
}

export const ROLE_LABELS: Record<Role, string> = {
  ROLE_STUDENT: 'Student',
  ROLE_PLACEMENT_OFFICER: 'Placement Officer',
  ROLE_ADMIN: 'Administrator',
}
