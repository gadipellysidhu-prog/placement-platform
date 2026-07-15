import { Gift, Building2, Calendar, Wallet } from 'lucide-react'
import { PageHeader } from '@/shared/ui/page-header'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/ui/card'
import { StatusBadge } from '@/shared/ui/status-badge'
import { Skeleton } from '@/shared/ui/skeleton'
import { ErrorState } from '@/shared/ui/error-state'
import { EmptyState } from '@/shared/ui/empty-state'
import { formatCTC, formatDate } from '@/utils/format'
import { useMyOffers } from '../hooks/use-offers'
import { OfferActions } from '../components/OfferActions'

export default function MyOffersPage() {
  const { data, isLoading, isError, refetch } = useMyOffers()

  return (
    <div className="space-y-6">
      <PageHeader
        title="My Offers"
        description="Review and respond to the placement offers extended to you."
      />

      {isLoading && (
        <div className="grid gap-4">
          <Skeleton className="h-40 rounded-xl" />
          <Skeleton className="h-40 rounded-xl" />
        </div>
      )}

      {isError && <ErrorState title="Couldn’t load your offers" onRetry={() => refetch()} />}

      {!isLoading && !isError && data && data.length === 0 && (
        <EmptyState
          icon={<Gift />}
          title="No offers yet"
          description="When a placement officer extends an offer, it will appear here."
        />
      )}

      {!isLoading && !isError && data && data.length > 0 && (
        <ul className="grid gap-4">
          {data.map((offer) => (
            <li key={offer.id}>
              <Card>
                <CardHeader className="flex flex-row items-start justify-between gap-4 space-y-0">
                  <div className="min-w-0 space-y-1">
                    <CardTitle className="flex items-center gap-2 truncate">
                      <Building2 className="h-4 w-4 shrink-0 text-muted-foreground" />
                      {offer.companyName}
                    </CardTitle>
                    <p className="text-sm text-muted-foreground">
                      Roll no. {offer.studentRollNumber}
                    </p>
                  </div>
                  <StatusBadge status={offer.status} />
                </CardHeader>
                <CardContent className="space-y-4">
                  <div className="grid gap-3 sm:grid-cols-2">
                    <Detail
                      icon={<Wallet className="h-4 w-4" />}
                      label="Package"
                      value={formatCTC(offer.ctc)}
                    />
                    <Detail
                      icon={<Calendar className="h-4 w-4" />}
                      label="Joining"
                      value={formatDate(offer.joiningDate)}
                    />
                  </div>

                  {offer.status === 'PENDING' ? (
                    <OfferActions offer={offer} fullWidth />
                  ) : (
                    <p className="text-sm text-muted-foreground">
                      This offer is {offer.status.toLowerCase()} and can no longer be changed.
                    </p>
                  )}
                </CardContent>
              </Card>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}

function Detail({ icon, label, value }: { icon: React.ReactNode; label: string; value: string }) {
  return (
    <div className="flex items-center gap-2">
      <span className="text-muted-foreground">{icon}</span>
      <span className="text-sm text-muted-foreground">{label}:</span>
      <span className="text-sm font-medium text-foreground">{value}</span>
    </div>
  )
}
