import { useQuery } from '@tanstack/react-query'
import { dashboardApi, queryKeys } from '@/lib/api'

export function useDashboardSummary() {
  return useQuery({
    queryKey: queryKeys.dashboard.summary(),
    queryFn: () => dashboardApi.summary(),
    staleTime: 60_000,
  })
}
