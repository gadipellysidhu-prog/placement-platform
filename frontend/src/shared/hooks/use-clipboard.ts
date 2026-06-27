import { useCallback, useState } from 'react'

interface UseClipboardReturn {
  copy: (text: string) => Promise<void>
  copied: boolean
}

/** Copies text to the clipboard and briefly sets `copied` to true. */
export function useClipboard(resetDelay = 2000): UseClipboardReturn {
  const [copied, setCopied] = useState(false)

  const copy = useCallback(
    async (text: string) => {
      if (!navigator.clipboard) return
      await navigator.clipboard.writeText(text)
      setCopied(true)
      setTimeout(() => setCopied(false), resetDelay)
    },
    [resetDelay],
  )

  return { copy, copied }
}
