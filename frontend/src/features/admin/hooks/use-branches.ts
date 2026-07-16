import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { branchesApi, queryKeys } from '@/lib/api'
import type {
  BranchResponse,
  CreateBranchRequest,
  ListBranchesParams,
  UpdateBranchRequest,
} from '@/lib/api'

/**
 * Branch catalogue. Administration passes `activeOnly: false` to include
 * deactivated branches; every other caller keeps the server default (active only).
 */
export function useBranches(params?: ListBranchesParams) {
  return useQuery({
    queryKey: queryKeys.branches.list(params as Record<string, unknown> | undefined),
    queryFn: () => branchesApi.list(params),
    staleTime: 30_000,
  })
}

/**
 * Branches feed selectors across the app (job posting eligibility, student profiles),
 * so any mutation invalidates the whole branches tree rather than a single list —
 * an activate/deactivate moves a branch in and out of the default active-only view.
 */
function useBranchMutation<TVariables>(mutationFn: (vars: TVariables) => Promise<BranchResponse>) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn,
    onSuccess: (updated: BranchResponse) => {
      qc.setQueryData(queryKeys.branches.detail(updated.id), updated)
      void qc.invalidateQueries({ queryKey: queryKeys.branches.all() })
    },
  })
}

export function useCreateBranch() {
  return useBranchMutation((data: CreateBranchRequest) => branchesApi.create(data))
}

export function useUpdateBranch(id: string) {
  return useBranchMutation((data: UpdateBranchRequest) => branchesApi.update(id, data))
}

export function useActivateBranch() {
  return useBranchMutation((id: string) => branchesApi.activate(id))
}

export function useDeactivateBranch() {
  return useBranchMutation((id: string) => branchesApi.deactivate(id))
}
