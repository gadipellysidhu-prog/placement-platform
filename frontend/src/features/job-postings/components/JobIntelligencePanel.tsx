import { useState } from 'react'
import { AlertTriangle, Bot, Check, ChevronDown, ChevronUp, RefreshCw } from 'lucide-react'
import { toast } from 'sonner'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/ui/card'
import { Badge } from '@/shared/ui/badge'
import { Button } from '@/shared/ui/button'
import { Spinner } from '@/shared/ui/spinner'
import { getApiErrorMessage } from '@/lib/api'
import type { RunStatus } from '@/lib/api'
import { cn } from '@/utils/cn'
import {
  useLatestJobIntelligenceRun,
  useRetryJobIntelligenceRun,
  useRunCompletionRefresh,
} from '../hooks/use-job-intelligence'

/** Pipeline stages in execution order, with officer-friendly labels. */
const STAGES: { status: RunStatus; label: string }[] = [
  { status: 'PENDING', label: 'Queued' },
  { status: 'FETCHING', label: 'Fetching official job' },
  { status: 'EXTRACTING', label: 'Running AI extraction' },
  { status: 'NORMALIZING', label: 'Normalizing skills & updating catalog' },
  { status: 'TAGGING', label: 'Attaching skills' },
  { status: 'PREDICTING_BRANCHES', label: 'Predicting branches' },
]

interface JobIntelligencePanelProps {
  postingId: string
}

/**
 * Live AI analysis panel on the posting detail page (officers only). Polls the
 * latest run with backoff, shows per-stage progress, a summary card once
 * finished, collapsible warnings, and a Retry action on failure. When the run
 * completes, posting/skills caches are invalidated so chips refresh in place.
 */
export default function JobIntelligencePanel({ postingId }: JobIntelligencePanelProps) {
  const [activityOpen, setActivityOpen] = useState(false)
  const { data: run, notAnalyzed, isLoading } = useLatestJobIntelligenceRun(postingId)
  const retry = useRetryJobIntelligenceRun(postingId)
  useRunCompletionRefresh(postingId, run)

  // Never analyzed → render nothing; the manual workflow stands on its own.
  if (notAnalyzed || (!run && !isLoading)) {
    return null
  }
  if (isLoading || !run) {
    return null
  }

  async function handleRetry() {
    try {
      await retry.mutateAsync(run!.id)
      toast.info('AI analysis restarted')
    } catch (err) {
      toast.error(getApiErrorMessage(err))
    }
  }

  const currentStageIndex = STAGES.findIndex((s) => s.status === run.status)
  const running = !run.terminal

  return (
    <Card aria-label="AI analysis">
      <CardHeader className="flex flex-row items-center justify-between space-y-0">
        <CardTitle className="flex items-center gap-2">
          <Bot className="h-5 w-5 text-primary" aria-hidden="true" />
          AI Analysis
        </CardTitle>
        {run.status === 'COMPLETED' && <Badge variant="success">Completed</Badge>}
        {run.status === 'FAILED' && <Badge variant="destructive">Failed</Badge>}
        {running && (
          <Badge variant="secondary" className="gap-1.5">
            <Spinner size="sm" /> Running
          </Badge>
        )}
      </CardHeader>

      <CardContent className="space-y-4" aria-live="polite">
        {/* Live stage progress while the pipeline runs */}
        {running && (
          <ol className="space-y-1.5" aria-label="Analysis progress">
            {STAGES.map((stage, index) => {
              const done = currentStageIndex > index
              const active = currentStageIndex === index
              return (
                <li
                  key={stage.status}
                  className={cn(
                    'flex items-center gap-2 text-sm',
                    done && 'text-muted-foreground',
                    active && 'font-medium text-foreground',
                    !done && !active && 'text-muted-foreground/60',
                  )}
                >
                  {done ? (
                    <Check className="h-4 w-4 text-success" aria-hidden="true" />
                  ) : active ? (
                    <Spinner size="sm" />
                  ) : (
                    <span className="inline-block h-4 w-4 rounded-full border border-border" />
                  )}
                  {stage.label}
                  {active && <span className="sr-only">(in progress)</span>}
                </li>
              )
            })}
          </ol>
        )}

        {/* Summary once terminal */}
        {run.status === 'COMPLETED' && (
          <dl className="grid grid-cols-2 gap-x-6 gap-y-2 text-sm sm:grid-cols-3">
            <SummaryItem label="Provider" value={run.provider ?? '—'} />
            <SummaryItem
              label="Confidence"
              value={run.confidence != null ? `${Math.round(run.confidence)}%` : '—'}
            />
            <SummaryItem
              label="Duration"
              value={run.durationMs != null ? `${(run.durationMs / 1000).toFixed(1)} s` : '—'}
            />
            <SummaryItem label="Skills extracted" value={String(run.skillsExtracted)} />
            <SummaryItem label="Skills attached" value={String(run.skillsTagged)} />
            <SummaryItem label="New catalog skills" value={String(run.skillsCreated)} />
            {run.predictedBranches.length > 0 && (
              <div className="col-span-full">
                <dt className="text-muted-foreground">Predicted branches</dt>
                <dd className="mt-1 flex flex-wrap gap-1.5">
                  {run.predictedBranches.map((branch) => (
                    <Badge key={branch} variant="outline">
                      {branch}
                    </Badge>
                  ))}
                </dd>
              </div>
            )}
          </dl>
        )}

        {run.status === 'FAILED' && (
          <div className="space-y-3">
            <p className="text-sm text-destructive" role="alert">
              {run.errorMessage ?? 'The analysis could not be completed.'}
            </p>
            <p className="text-sm text-muted-foreground">
              You can retry the analysis or continue tagging skills manually — nothing is blocked.
            </p>
            <Button variant="outline" size="sm" onClick={handleRetry} disabled={retry.isPending}>
              {retry.isPending ? (
                <Spinner size="sm" className="mr-1" />
              ) : (
                <RefreshCw className="h-4 w-4" />
              )}
              Retry AI Analysis
            </Button>
          </div>
        )}

        {/* Collapsible activity: source, model, attempts, warnings */}
        <div>
          <button
            type="button"
            className="flex items-center gap-1 text-xs font-medium text-muted-foreground hover:text-foreground"
            onClick={() => setActivityOpen((open) => !open)}
            aria-expanded={activityOpen}
          >
            {activityOpen ? (
              <ChevronUp className="h-3.5 w-3.5" aria-hidden="true" />
            ) : (
              <ChevronDown className="h-3.5 w-3.5" aria-hidden="true" />
            )}
            Activity details
          </button>
          {activityOpen && (
            <div className="mt-2 space-y-2 rounded-md border border-border p-3 text-xs text-muted-foreground">
              <p className="break-all">Source: {run.officialUrl}</p>
              {run.model && <p>Model: {run.model}</p>}
              {run.retryCount > 0 && <p>Retry attempts: {run.retryCount}</p>}
              {run.warnings.length > 0 && (
                <ul className="space-y-1" aria-label="Warnings">
                  {run.warnings.map((warning, index) => (
                    <li key={index} className="flex items-start gap-1.5">
                      <AlertTriangle
                        className="mt-0.5 h-3 w-3 shrink-0 text-warning"
                        aria-hidden="true"
                      />
                      {warning}
                    </li>
                  ))}
                </ul>
              )}
            </div>
          )}
        </div>
      </CardContent>
    </Card>
  )
}

function SummaryItem({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <dt className="text-muted-foreground">{label}</dt>
      <dd className="font-medium text-foreground">{value}</dd>
    </div>
  )
}
