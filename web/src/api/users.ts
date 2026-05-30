import { getData, patchData, postData } from './http'

export interface UserAdminItem {
  id: number
  account: string
  nickname: string
  email: string | null
  avatarUrl: string | null
  status: string
  role: string
  createdAt: string
  updatedAt: string
}

export interface CreateUserRequest {
  account: string
  nickname: string
  email?: string
  password: string
  role: string
}

export function fetchUsers(): Promise<UserAdminItem[]> {
  return getData<UserAdminItem[]>('/admin/users')
}

export function createUser(request: CreateUserRequest): Promise<UserAdminItem> {
  return postData<UserAdminItem, CreateUserRequest>('/admin/users', request)
}

export function updateUserStatus(account: string, status: string): Promise<UserAdminItem> {
  return patchData<UserAdminItem, { status: string }>(`/admin/users/${account}/status`, { status })
}
