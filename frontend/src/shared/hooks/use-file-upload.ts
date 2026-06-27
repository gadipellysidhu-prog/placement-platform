import { useCallback, useState } from 'react'
import { filesApi, getApiErrorMessage, type FileResponse } from '@/lib/api'

interface UseFileUploadReturn {
  upload: (file: File) => Promise<FileResponse | null>
  uploading: boolean
  progress: number
  error: string | null
  reset: () => void
}

/** Manages a single-file upload lifecycle against the backend file pipeline. */
export function useFileUpload(): UseFileUploadReturn {
  const [uploading, setUploading] = useState(false)
  const [progress, setProgress] = useState(0)
  const [error, setError] = useState<string | null>(null)

  const upload = useCallback(async (file: File): Promise<FileResponse | null> => {
    setUploading(true)
    setProgress(0)
    setError(null)

    try {
      const result = await filesApi.upload(file, (pct) => setProgress(pct))
      setProgress(100)
      return result
    } catch (err) {
      setError(getApiErrorMessage(err))
      return null
    } finally {
      setUploading(false)
    }
  }, [])

  const reset = useCallback(() => {
    setUploading(false)
    setProgress(0)
    setError(null)
  }, [])

  return { upload, uploading, progress, error, reset }
}
