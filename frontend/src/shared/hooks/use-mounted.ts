import { useEffect, useRef } from 'react'

/** Returns a ref that is true after the component has mounted. Prevents stale closure issues. */
export function useMounted() {
  const mounted = useRef(false)
  useEffect(() => {
    mounted.current = true
    return () => {
      mounted.current = false
    }
  }, [])
  return mounted
}
