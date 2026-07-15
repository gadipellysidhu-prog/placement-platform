import type { AxiosRequestConfig } from 'axios'
import { apiClient } from '@/lib/axios'

/** Mirrors the backend {@code FileScanStatus} enum exactly (PENDING/CLEAN/INFECTED/FAILED). */
export type FileScanStatus = 'PENDING' | 'CLEAN' | 'INFECTED' | 'FAILED'

/** Mirrors the backend {@code FileUploadResponse} record. */
export interface FileResponse {
  id: string
  filename: string
  contentType: string
  sizeBytes: number
  sha256Hash: string
  scanStatus: FileScanStatus
  quarantined: boolean
  uploadedBy: string
  uploadedAt: string
}

/** Mirrors the backend {@code FileDownloadLinkResponse} record. */
export interface FileDownloadLinkResponse {
  id: string
  url: string
  signed: boolean
}

export const filesApi = {
  /**
   * Uploads a single file via multipart/form-data (field name `file`).
   *
   * The Content-Type header is intentionally left unset so the browser/axios can
   * generate `multipart/form-data` *with the required boundary* — setting it by hand
   * (without a boundary) makes the server-side multipart parser reject the request.
   */
  upload: (file: File, onProgress?: (percent: number) => void) => {
    const form = new FormData()
    form.append('file', file)
    const config: AxiosRequestConfig = {
      // Undefined removes the instance's default JSON content-type; axios then derives
      // the multipart boundary from the FormData body.
      headers: { 'Content-Type': undefined } as AxiosRequestConfig['headers'],
      onUploadProgress: (event) => {
        if (onProgress && event.total) {
          onProgress(Math.round((event.loaded / event.total) * 100))
        }
      },
    }
    return apiClient.post<FileResponse>('/api/files/upload', form, config).then((r) => r.data)
  },

  download: (id: string) =>
    apiClient.get<Blob>(`/api/files/${id}`, { responseType: 'blob' }).then((r) => r.data),

  /** Resolve a (possibly signed) download URL for a stored file. */
  link: (id: string) =>
    apiClient.get<FileDownloadLinkResponse>(`/api/files/${id}/link`).then((r) => r.data),

  /** Delete a stored file. Restricted to ADMIN / PLACEMENT_OFFICER on the backend. */
  delete: (id: string) => apiClient.delete<void>(`/api/files/${id}`).then((r) => r.data),
}
