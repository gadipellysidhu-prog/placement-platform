import { StatusBadge } from '@/shared/ui/status-badge'
import { formatDateTime } from '@/utils/format'
import type { JobApplicationResponse } from '@/lib/api'
import { buildApplicationTimeline } from '../applications.timeline'

interface ApplicationTimelineProps {
  application: JobApplicationResponse
}

/** Reusable vertical timeline shown on My Applications and the application detail page. */
export function ApplicationTimeline({ application }: ApplicationTimelineProps) {
  const events = buildApplicationTimeline(application)

  return (
    <ol className="space-y-4" aria-label="Application timeline">
      {events.map((event, index) => {
        const isLast = index === events.length - 1
        return (
          <li key={`${event.status}-${event.at}`} className="relative flex gap-3">
            <div className="flex flex-col items-center">
              <span
                className={
                  'mt-1 h-2.5 w-2.5 shrink-0 rounded-full ' +
                  (isLast ? 'bg-primary' : 'bg-muted-foreground/50')
                }
                aria-hidden="true"
              />
              {!isLast && <span className="mt-1 w-px flex-1 bg-border" aria-hidden="true" />}
            </div>
            <div className="flex flex-col gap-1 pb-1">
              <StatusBadge status={event.status} />
              <time className="text-xs text-muted-foreground">{formatDateTime(event.at)}</time>
            </div>
          </li>
        )
      })}
    </ol>
  )
}
