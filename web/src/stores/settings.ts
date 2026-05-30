import { defineStore } from 'pinia'
import {
  createBoardTemplate,
  deleteBoardTemplate,
  fetchBoardTemplates,
  reorderBoardTemplates,
  updateBoardTemplate,
  type BoardColumnTemplate,
  type CreateBoardColumnTemplateRequest,
  type SaveBoardColumnTemplateRequest,
  type UpdateBoardColumnTemplateRequest,
} from '../api/settings'

export const useSettingsStore = defineStore('settings', {
  state: () => ({
    boardTemplates: [] as BoardColumnTemplate[],
    loading: false,
    error: null as string | null,
  }),
  actions: {
    async loadBoardTemplates() {
      this.loading = true
      this.error = null
      try {
        this.boardTemplates = await fetchBoardTemplates()
      } catch (error) {
        this.error = 'Board templates failed to load'
        throw error
      } finally {
        this.loading = false
      }
    },
    async saveBoardTemplate(request: SaveBoardColumnTemplateRequest) {
      this.error = null
      const templateKey = request.templateKey?.trim()
      const existing = templateKey
        ? this.boardTemplates.find((template) => template.templateKey === templateKey)
        : undefined

      try {
        let saved: BoardColumnTemplate
        if (existing && templateKey) {
          const { templateKey: _templateKey, ...updateRequest } = request
          saved = await updateBoardTemplate(templateKey, updateRequest as UpdateBoardColumnTemplateRequest)
          this.boardTemplates = this.boardTemplates.map((template) =>
            template.templateKey === templateKey ? saved : template,
          )
        } else {
          if (!templateKey) {
            throw new Error('Template key is required')
          }
          const createRequest: CreateBoardColumnTemplateRequest = { ...request, templateKey }
          saved = await createBoardTemplate(createRequest)
          this.boardTemplates = [...this.boardTemplates, saved].sort((left, right) => left.sortOrder - right.sortOrder)
        }
        return saved
      } catch (error) {
        this.error = error instanceof Error && error.message === 'Template key is required'
          ? error.message
          : 'Board template save failed'
        throw error
      }
    },
    async reorder(templateKeys: string[]) {
      this.error = null
      try {
        this.boardTemplates = await reorderBoardTemplates(templateKeys)
      } catch (error) {
        this.error = 'Board templates reorder failed'
        throw error
      }
    },
    async remove(templateKey: string) {
      this.error = null
      try {
        await deleteBoardTemplate(templateKey)
        this.boardTemplates = this.boardTemplates.filter((template) => template.templateKey !== templateKey)
      } catch (error) {
        this.error = 'Board template delete failed'
        throw error
      }
    },
  },
})
