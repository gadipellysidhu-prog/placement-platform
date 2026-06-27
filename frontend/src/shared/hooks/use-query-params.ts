import { useCallback } from 'react'
import { useSearchParams } from 'react-router-dom'

type ParamValue = string | number | boolean | null | undefined

/** Reads and writes URL search params as typed values. */
export function useQueryParams<T extends Record<string, ParamValue>>() {
  const [searchParams, setSearchParams] = useSearchParams()

  const getParam = useCallback(
    (key: keyof T): string | null => searchParams.get(key as string),
    [searchParams],
  )

  const setParam = useCallback(
    (key: keyof T, value: ParamValue) => {
      setSearchParams((prev) => {
        const next = new URLSearchParams(prev)
        if (value === null || value === undefined || value === '') {
          next.delete(key as string)
        } else {
          next.set(key as string, String(value))
        }
        return next
      })
    },
    [setSearchParams],
  )

  const setParams = useCallback(
    (updates: Partial<T>) => {
      setSearchParams((prev) => {
        const next = new URLSearchParams(prev)
        for (const [key, value] of Object.entries(updates)) {
          if (value === null || value === undefined || value === '') {
            next.delete(key)
          } else {
            next.set(key, String(value))
          }
        }
        return next
      })
    },
    [setSearchParams],
  )

  const clearParams = useCallback(
    (keys?: (keyof T)[]) => {
      setSearchParams((prev) => {
        const next = new URLSearchParams(prev)
        if (keys) {
          keys.forEach((k) => next.delete(k as string))
        } else {
          next.forEach((_, k) => next.delete(k))
        }
        return next
      })
    },
    [setSearchParams],
  )

  return { searchParams, getParam, setParam, setParams, clearParams }
}
