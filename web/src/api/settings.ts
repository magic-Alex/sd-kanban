import { deleteData, getData, patchData, postData } from './http'

export interface BoardColumnTemplate {
  id: number
  templateKey: string
  nameZh: string
  nameEn: string
  displayName: string
  color: string
  sortOrder: number
  wipLimit: number | null
  isDone: boolean
}

interface BoardColumnTemplatePayload {
  nameZh: string
  nameEn: string
  color: string
  wipLimit?: number | null
  isDone: boolean
}

export interface CreateBoardColumnTemplateRequest extends BoardColumnTemplatePayload {
  templateKey: string
}

export interface UpdateBoardColumnTemplateRequest extends BoardColumnTemplatePayload {}

export interface SaveBoardColumnTemplateRequest extends BoardColumnTemplatePayload {
  templateKey?: string
}

export function fetchBoardTemplates(): Promise<BoardColumnTemplate[]> {
  return getData<BoardColumnTemplate[]>('/admin/board-templates')
}

export function createBoardTemplate(request: CreateBoardColumnTemplateRequest): Promise<BoardColumnTemplate> {
  return postData<BoardColumnTemplate, CreateBoardColumnTemplateRequest>('/admin/board-templates', request)
}

export function updateBoardTemplate(
  templateKey: string,
  request: UpdateBoardColumnTemplateRequest,
): Promise<BoardColumnTemplate> {
  return patchData<BoardColumnTemplate, UpdateBoardColumnTemplateRequest>(
    `/admin/board-templates/${encodeURIComponent(templateKey)}`,
    request,
  )
}

export function reorderBoardTemplates(templateKeys: string[]): Promise<BoardColumnTemplate[]> {
  return patchData<BoardColumnTemplate[], { templateKeys: string[] }>('/admin/board-templates/reorder', {
    templateKeys,
  })
}

export function deleteBoardTemplate(templateKey: string): Promise<void> {
  return deleteData<void>(`/admin/board-templates/${encodeURIComponent(templateKey)}`)
}
