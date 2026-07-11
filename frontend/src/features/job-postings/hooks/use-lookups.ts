import { useQuery } from '@tanstack/react-query'
import { branchesApi, companiesApi, queryKeys, skillsApi } from '@/lib/api'
import type { ListSkillsParams } from '@/lib/api/skills.api'
import type { PageParams } from '@/types'

/**
 * Shared read-only lookups backing the job-posting forms and tagging UI. These wrap the
 * canonical `*.api.ts` clients so no feature re-implements the fetch logic. Lookup data
 * changes rarely, so a longer stale time keeps the selectors snappy.
 */

const LOOKUP_STALE_TIME = 5 * 60_000

/** Full branch list (unpaginated) — backend returns every branch. */
export function useBranches() {
  return useQuery({
    queryKey: queryKeys.branches.list(),
    queryFn: () => branchesApi.list(),
    staleTime: LOOKUP_STALE_TIME,
  })
}

/** Skill list, optionally filtered by category / verified. */
export function useSkills(params?: ListSkillsParams) {
  return useQuery({
    queryKey: queryKeys.skills.list(params as Record<string, unknown> | undefined),
    queryFn: () => skillsApi.list(params),
    staleTime: LOOKUP_STALE_TIME,
  })
}

/**
 * Companies for the create/edit posting selector. A large page size is requested so the
 * selector shows the full roster without its own pagination — mirrors how the backend
 * caps company volume for a single institution.
 */
export function useCompaniesLookup(params?: PageParams) {
  return useQuery({
    queryKey: queryKeys.companies.list({ ...(params ?? {}), lookup: true }),
    queryFn: () => companiesApi.list({ page: 0, size: 200, ...params }),
    staleTime: LOOKUP_STALE_TIME,
  })
}
