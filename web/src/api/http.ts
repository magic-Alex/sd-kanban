import axios from 'axios'

export interface ApiResponse<T> {
  success: boolean
  data: T
  code: string | null
  message: string | null
  fieldErrors: Record<string, string> | null
}

export const http = axios.create({
  baseURL: '/api',
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('sd-kanban-token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

export async function getData<T>(path: string): Promise<T> {
  const response = await http.get<ApiResponse<T>>(path)
  return response.data.data
}

export async function postData<T, B>(path: string, body: B): Promise<T> {
  const response = await http.post<ApiResponse<T>>(path, body)
  return response.data.data
}

export async function patchData<T, B>(path: string, body: B): Promise<T> {
  const response = await http.patch<ApiResponse<T>>(path, body)
  return response.data.data
}
