import { defineStore } from 'pinia'
import {
  createBoardTemplate,
  deleteBoardTemplate,
  fetchBoardTemplates,
  reorderBoardTemplates,
  updateBoardTemplate,
  type BoardColumnTemplate,
  type SaveBoardColumnTemplateRequest,
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
      const templateKey = request.templateKey?.trim()
      const existing = templateKey
        ? this.boardTemplates.find((template) => template.templateKey === templateKey)
        : undefined

      let saved: BoardColumnTemplate
      if (existing && templateKey) {
        const { templateKey: _templateKey, ...updateRequest } = request
        saved = await updateBoardTemplate(templateKey, updateRequest)
        this.boardTemplates = this.boardTemplates.map((template) =>
          template.templateKey === templateKey ? saved : template,
        )
      } else {
        saved = await createBoardTemplate(request)
        this.boardTemplates = [...this.boardTemplates, saved].sort((left, right) => left.sortOrder - right.sortOrder)
      }
      return saved
    },
    async reorder(templateKeys: string[]) {
      this.boardTemplates = await reorderBoardTemplates(templateKeys)
    },
    async remove(templateKey: string) {
      await deleteBoardTemplate(templateKey)
      this.boardTemplates = this.boardTemplates.filter((template) => template.templateKey !== templateKey)
    },
  },
})
