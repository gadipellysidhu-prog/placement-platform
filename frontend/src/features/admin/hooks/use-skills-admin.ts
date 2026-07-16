import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { skillsApi, queryKeys } from '@/lib/api'
import type {
  CreateSkillRequest,
  ListSkillsParams,
  SkillResponse,
  UpdateSkillRequest,
} from '@/lib/api'

export function useSkills(params?: ListSkillsParams) {
  return useQuery({
    queryKey: queryKeys.skills.list(params as Record<string, unknown> | undefined),
    queryFn: () => skillsApi.list(params),
    staleTime: 30_000,
  })
}

/** Detail projection — the only response carrying the skill's aliases. */
export function useSkill(id: string) {
  return useQuery({
    queryKey: queryKeys.skills.detail(id),
    queryFn: () => skillsApi.getById(id),
    staleTime: 30_000,
    enabled: !!id,
  })
}

/**
 * Skills feed the tagging pickers and the catalog search, so a mutation invalidates
 * the whole skills tree. The mutation responses use the list projection (aliases
 * null), so the detail entry is refetched rather than seeded from them.
 */
function useSkillMutation<TVariables>(mutationFn: (vars: TVariables) => Promise<SkillResponse>) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn,
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: queryKeys.skills.all() })
    },
  })
}

export function useCreateSkill() {
  return useSkillMutation((data: CreateSkillRequest) => skillsApi.create(data))
}

export function useUpdateSkill(id: string) {
  return useSkillMutation((data: UpdateSkillRequest) => skillsApi.update(id, data))
}

export function useVerifySkill() {
  return useSkillMutation((id: string) => skillsApi.verify(id))
}
