import * as React from 'react'
import { Card, CardContent } from './card'
import { Skeleton } from './skeleton'
import { cn } from '@/utils/cn'

interface StatCardProps {
  title: string
  value: React.ReactNode
  icon?: React.ReactNode
  description?: string
  className?: string
  isLoading?: boolean
}

function StatCard({ title, value, icon, description, className, isLoading }: StatCardProps) {
  if (isLoading) {
    return (
      <Card className={className}>
        <CardContent className="p-6">
          <div className="flex items-start justify-between">
            <div className="space-y-2 flex-1">
              <Skeleton className="h-4 w-24" />
              <Skeleton className="h-8 w-16" />
              {description !== undefined && <Skeleton className="h-3 w-32" />}
            </div>
            {icon && <Skeleton className="h-9 w-9 rounded-lg" />}
          </div>
        </CardContent>
      </Card>
    )
  }

  return (
    <Card className={className}>
      <CardContent className="p-6">
        <div className="flex items-start justify-between gap-4">
          <div className="space-y-1 min-w-0">
            <p className="text-sm font-medium text-muted-foreground">{title}</p>
            <p className="text-2xl font-bold text-foreground">{value}</p>
            {description && <p className="text-xs text-muted-foreground truncate">{description}</p>}
          </div>
          {icon && (
            <div
              className={cn(
                'flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-primary/10',
                '[&_svg]:h-5 [&_svg]:w-5 [&_svg]:text-primary',
              )}
              aria-hidden="true"
            >
              {icon}
            </div>
          )}
        </div>
      </CardContent>
    </Card>
  )
}

export { StatCard }
