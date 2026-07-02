import { apiClient } from '@/lib/axios'

export interface FileResponse {
  id: string
  filename: string
  contentType: string
  sizeBytes: number
  sha256Hash: string
  scanStatus: 'CLEAN' | 'INFECTED' | 'SCAN_ERROR' | 'PENDING'
  quarantined: boolean
  uploadedBy: string
  uploadedAt: string
}

export const filesApi = {
  upload: (file: File, onProgress?: (percent: number) => void) => {
    const form = new FormData()
    form.append('file', file)
    return apiClient
      .post<FileResponse>('/api/files/upload', form, {
        headers: { 'Content-Type': 'multipart/form-data' },
        onUploadProgress: (event) => {
          if (onProgress && event.total) {
            onProgress(Math.round((event.loaded / event.total) * 100))
          }
        },
      })
      .then((r) => r.data)
  },

  download: (id: string) =>
    apiClient.get<Blob>(`/api/files/${id}`, { responseType: 'blob' }).then((r) => r.data),

  delete: (id: string) => apiClient.delete<void>(`/api/files/${id}`).then((r) => r.data),
}
