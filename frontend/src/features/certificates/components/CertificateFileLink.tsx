import { useState } from 'react'
import { FileDown, ExternalLink } from 'lucide-react'
import { Button } from '@/shared/ui/button'
import { Spinner } from '@/shared/ui/spinner'
import { filesApi, getApiErrorMessage } from '@/lib/api'
import { useToast } from '@/shared/hooks/use-toast'

interface CertificateFileLinkProps {
  /** The certificate's fileKey — the uploaded file's id. */
  fileKey: string | null
  className?: string
}

/**
 * Downloads a certificate's document through the authenticated file endpoint and opens
 * it in a new tab. A plain anchor can't be used because `GET /api/files/{id}` requires
 * the Bearer token, so the blob is fetched via the axios client (which attaches it) and
 * shown via an object URL.
 */
export function CertificateFileLink({ fileKey, className }: CertificateFileLinkProps) {
  const [loading, setLoading] = useState(false)
  const toast = useToast()

  if (!fileKey) {
    return <span className="text-sm text-muted-foreground">No document</span>
  }

  async function handleView() {
    setLoading(true)
    try {
      const blob = await filesApi.download(fileKey!)
      const url = URL.createObjectURL(blob)
      window.open(url, '_blank', 'noopener,noreferrer')
      // Revoke on the next tick so the new tab has time to read the blob.
      setTimeout(() => URL.revokeObjectURL(url), 60_000)
    } catch (err) {
      toast.error('Could not open document', getApiErrorMessage(err))
    } finally {
      setLoading(false)
    }
  }

  return (
    <Button
      type="button"
      variant="outline"
      size="sm"
      onClick={handleView}
      disabled={loading}
      className={className}
    >
      {loading ? <Spinner size="sm" /> : <FileDown className="h-4 w-4" />}
      View document
      {!loading && <ExternalLink className="h-3 w-3 opacity-60" />}
    </Button>
  )
}
