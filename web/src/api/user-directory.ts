import { getData } from './http'

export interface UserDirectoryEntry {
  id: number
  account: string
  nickname: string
  avatarUrl: string | null
}

export function searchActiveUsers(keyword: string): Promise<UserDirectoryEntry[]> {
  const trimmedKeyword = keyword.trim()
  if (!trimmedKeyword) {
    return Promise.resolve([])
  }
  return getData<UserDirectoryEntry[]>(`/users/directory?keyword=${encodeURIComponent(trimmedKeyword)}`)
}
