import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { academicYearsApi, queryKeys } from '@/lib/api'
import type {
  AcademicYearResponse,
  CreateAcademicYearRequest,
  UpdateAcademicYearRequest,
} from '@/lib/api'
import type { PageParams } from '@/types'

export function useAcademicYears(params?: PageParams) {
  return useQuery({
    queryKey: queryKeys.academicYears.list(params as Record<string, unknown> | undefined),
    queryFn: () => academicYearsApi.list(params),
    staleTime: 30_000,
  })
}

/**
 * Activation is exclusive server-side — activating one year deactivates the other —
 * so the whole tree is invalidated rather than patching the two affected rows.
 */
function useAcademicYearMutation<TVariables>(
  mutationFn: (vars: TVariables) => Promise<AcademicYearResponse>,
) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn,
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: queryKeys.academicYears.all() })
    },
  })
}

export function useCreateAcademicYear() {
  return useAcademicYearMutation((data: CreateAcademicYearRequest) => academicYearsApi.create(data))
}

export function useUpdateAcademicYear(id: string) {
  return useAcademicYearMutation((data: UpdateAcademicYearRequest) =>
    academicYearsApi.update(id, data),
  )
}

export function useActivateAcademicYear() {
  return useAcademicYearMutation((id: string) => academicYearsApi.activate(id))
}
