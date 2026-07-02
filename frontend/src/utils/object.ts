/** Remove undefined and null values from a shallow object. */
export function omitNullish<T extends Record<string, unknown>>(
  obj: T,
): { [K in keyof T]: NonNullable<T[K]> } {
  return Object.fromEntries(
    Object.entries(obj).filter(([, v]) => v !== null && v !== undefined),
  ) as { [K in keyof T]: NonNullable<T[K]> }
}

/** Pick specific keys from an object. */
export function pick<T extends object, K extends keyof T>(obj: T, keys: K[]): Pick<T, K> {
  return Object.fromEntries(keys.map((k) => [k, obj[k]])) as Pick<T, K>
}

/** Omit specific keys from an object. */
export function omit<T extends object, K extends keyof T>(obj: T, keys: K[]): Omit<T, K> {
  const set = new Set(keys as string[])
  return Object.fromEntries(Object.entries(obj).filter(([k]) => !set.has(k))) as Omit<T, K>
}
