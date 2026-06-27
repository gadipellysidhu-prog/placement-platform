export type Role = 'ROLE_STUDENT' | 'ROLE_PLACEMENT_OFFICER' | 'ROLE_ADMIN'

export interface User {
  email: string
  role: Role
}

export interface AuthTokens {
  accessToken: string
  refreshToken: string
  accessTokenExpiresIn: number
  tokenType: 'Bearer'
}
