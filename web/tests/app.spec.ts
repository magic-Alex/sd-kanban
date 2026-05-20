import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import App from '../src/App.vue'
import router from '../src/router'

describe('app scaffold', () => {
  it('renders the scaffold UI through the app router', async () => {
    router.push('/')
    await router.isReady()

    const wrapper = mount(App, {
      global: {
        plugins: [router],
      },
    })

    expect(wrapper.text()).toContain('SD Kanban')
    expect(wrapper.text()).toContain('Project board scaffold')
  })
})
