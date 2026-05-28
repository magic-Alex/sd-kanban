import { afterEach, describe, expect, it, vi } from 'vitest'

async function loadConfig() {
  vi.resetModules()
  return (await import('../vite.config')).default
}

describe('vite config', () => {
  afterEach(() => {
    vi.unstubAllEnvs()
  })

  it('allows runtime overrides for dev port and API proxy target', async () => {
    vi.stubEnv('FRONTEND_PORT', '8202')
    vi.stubEnv('API_PROXY_TARGET', 'http://localhost:8201')

    const config = await loadConfig()

    expect(config.server?.port).toBe(8202)
    expect(config.server?.proxy?.['/api']).toMatchObject({
      target: 'http://localhost:8201',
      changeOrigin: true,
    })
  })
})
