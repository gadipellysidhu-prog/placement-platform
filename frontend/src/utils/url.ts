/** Build a URL string with query params, omitting null/undefined values. */
export function buildUrl(
  base: string,
  params: Record<string, string | number | boolean | null | undefined>,
): string {
  const query = new URLSearchParams()
  for (const [key, value] of Object.entries(params)) {
    if (value !== null && value !== undefined && value !== '') {
      query.set(key, String(value))
    }
  }
  const qs = query.toString()
  return qs ? `${base}?${qs}` : base
}

/** Extract the file extension from a filename or URL. */
export function fileExtension(name: string): string {
  return name.split('.').pop()?.toLowerCase() ?? ''
}

/** Safely construct the download URL for a backend file by ID. */
export function fileDownloadUrl(apiBase: string, fileId: string): string {
  return `${apiBase}/api/files/${fileId}`
}
