import { useState } from 'react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { http, HttpResponse } from 'msw'
import { renderWithProviders, screen, waitFor } from '@/test'
import { server } from '@/test/msw/server'
import { API_BASE_URL, mockFileResponse } from '@/test'
import { FileUploadField } from './file-upload-field'
import type { FileResponse } from '@/lib/api'

/** Controlled harness — FileUploadField is a controlled component (value + onChange). */
function Harness({ onChangeSpy }: { onChangeSpy?: (f: FileResponse | null) => void }) {
  const [value, setValue] = useState<FileResponse | null>(null)
  return (
    <FileUploadField
      value={value}
      onChange={(f) => {
        setValue(f)
        onChangeSpy?.(f)
      }}
    />
  )
}

function fileInput(container: HTMLElement): HTMLInputElement {
  const input = container.querySelector('input[type="file"]')
  if (!input) throw new Error('file input not found')
  return input as HTMLInputElement
}

function pdf(name = 'certificate.pdf') {
  return new File(['%PDF-1.4 dummy'], name, { type: 'application/pdf' })
}

beforeEach(() => {
  sessionStorage.clear()
})

describe('FileUploadField', () => {
  it('uploads a picked file and returns the metadata, then shows the success state', async () => {
    const onChangeSpy = vi.fn()
    const { container, user } = renderWithProviders(<Harness onChangeSpy={onChangeSpy} />)

    await user.upload(fileInput(container), pdf())

    // Success state renders the uploaded filename and its clean scan status.
    expect(await screen.findByTestId('file-upload-success')).toBeInTheDocument()
    expect(screen.getByText('certificate.pdf')).toBeInTheDocument()
    expect(screen.getByText('Scanned clean')).toBeInTheDocument()

    // Parent received the backend metadata (used as the certificate fileKey).
    await waitFor(() => expect(onChangeSpy).toHaveBeenCalledWith(mockFileResponse))
  })

  it('replaces a file: best-effort deletes the previous upload and returns to the picker', async () => {
    let deleteCalled = false
    server.use(
      http.delete(`${API_BASE_URL}/api/files/:id`, () => {
        deleteCalled = true
        return new HttpResponse(null, { status: 204 })
      }),
    )

    const { container, user } = renderWithProviders(<Harness />)
    await user.upload(fileInput(container), pdf())
    await screen.findByTestId('file-upload-success')

    await user.click(screen.getByRole('button', { name: /replace/i }))

    // Back to the picker; previous upload was cleaned up.
    expect(await screen.findByText(/drag & drop or click to upload/i)).toBeInTheDocument()
    await waitFor(() => expect(deleteCalled).toBe(true))
  })

  it('surfaces a virus-scan rejection (422 ProblemDetail) and offers a retry', async () => {
    server.use(
      http.post(`${API_BASE_URL}/api/files/upload`, () =>
        HttpResponse.json(
          {
            title: 'Unprocessable Entity',
            status: 422,
            detail:
              "Malicious content detected in file 'certificate.pdf'. Upload rejected and file quarantined.",
          },
          { status: 422 },
        ),
      ),
    )

    const { container, user } = renderWithProviders(<Harness />)
    await user.upload(fileInput(container), pdf())

    expect(await screen.findByText(/malicious content detected/i)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /retry/i })).toBeInTheDocument()
    // No success state on rejection.
    expect(screen.queryByTestId('file-upload-success')).not.toBeInTheDocument()
  })
})
