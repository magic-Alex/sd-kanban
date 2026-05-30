import { getData, postData } from './http'

export interface LoginRequest {
  account: string
  password: string
}

export interface UserSummary {
  id: number
  account: string
  nickname: string
  email: string | null
  avatarUrl: string | null
  role?: string | null
}

export interface LoginResponse {
  token: string
  user: UserSummary
}

export function login(request: LoginRequest): Promise<LoginResponse> {
  return postData<LoginResponse, LoginRequest>('/auth/login', request)
}

export function fetchCurrentUser(): Promise<UserSummary> {
  return getData<UserSummary>('/auth/me')
}
