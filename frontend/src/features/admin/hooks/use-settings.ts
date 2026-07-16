import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { settingsApi, queryKeys } from '@/lib/api'
import type { ListSettingsParams, SettingResponse, SettingUpsertRequest } from '@/lib/api'

export function useSettings(params?: ListSettingsParams) {
  return useQuery({
    queryKey: queryKeys.settings.list(params as Record<string, unknown> | undefined),
    queryFn: () => settingsApi.list(params),
    staleTime: 30_000,
  })
}

/**
 * Create and edit share one endpoint — the backend upserts on
 * (settingKey + academicYearId). A new key changes the listing; an edited value
 * changes the row, so the whole settings tree is invalidated either way.
 */
export function useUpsertSetting() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: SettingUpsertRequest) => settingsApi.upsert(data),
    onSuccess: (saved: SettingResponse) => {
      qc.setQueryData(queryKeys.settings.detail(saved.id), saved)
      void qc.invalidateQueries({ queryKey: queryKeys.settings.all() })
    },
  })
}

export function useDeleteSetting() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => settingsApi.delete(id),
    onSuccess: (_data, id) => {
      qc.removeQueries({ queryKey: queryKeys.settings.detail(id) })
      void qc.invalidateQueries({ queryKey: queryKeys.settings.all() })
    },
  })
}
