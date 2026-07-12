import { useQuery } from '@tanstack/react-query'
import { skillsApi, queryKeys } from '@/lib/api'
import { useDebounce } from '@/shared/hooks/use-debounce'

/**
 * Debounced intelligent skill search against the catalog search endpoint
 * (names, aliases, abbreviations, fuzzy — ranked server-side, never alphabetical).
 */
export function useSkillSearch(rawQuery: string) {
  const query = useDebounce(rawQuery.trim(), 250)

  return useQuery({
    queryKey: queryKeys.skills.search(query),
    queryFn: () => skillsApi.search(query),
    enabled: query.length > 0,
    staleTime: 60_000, // catalog searches are cache-friendly
    placeholderData: (previous) => previous, // keep last results while typing
  })
}
