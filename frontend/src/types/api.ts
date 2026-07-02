/**
 * RFC 7807 ProblemDetail — mirrors the backend GlobalExceptionHandler error format exactly.
 */
export interface ProblemDetail {
  type?: string
  title: string
  status: number
  detail?: string
  errors?: string[]
}

export interface ApiError {
  status: number
  problem: ProblemDetail
  message: string
}

/** Spring Data Page<T> */
export interface Page<T> {
  content: T[]
  pageable: {
    pageNumber: number
    pageSize: number
    sort: { sorted: boolean }
  }
  totalElements: number
  totalPages: number
  last: boolean
  first: boolean
  numberOfElements: number
  size: number
  number: number
  empty: boolean
}

export interface PageParams {
  page?: number
  size?: number
  sort?: string
}
