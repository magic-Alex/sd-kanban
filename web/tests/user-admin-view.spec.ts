import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { enableAutoUnmount, flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import UserAdminView from '../src/views/UserAdminView.vue'
import { createUser, fetchUsers, updateUserStatus } from '../src/api/users'

vi.mock('../src/api/users', () => ({
  createUser: vi.fn(),
  fetchUsers: vi.fn(),
  updateUserStatus: vi.fn(),
}))

const users = [
  {
    id: 1,
    account: 'sd-robot',
    nickname: '系统管理员',
    email: null,
    avatarUrl: null,
    status: 'ACTIVE',
    role: 'ADMIN',
    createdAt: '2026-05-29T10:00:00',
    updatedAt: '2026-05-29T10:00:00',
  },
]

enableAutoUnmount(afterEach)

describe('UserAdminView', () => {
  beforeEach(() => {
    document.body.innerHTML = ''
    setActivePinia(createPinia())
    vi.mocked(fetchUsers).mockReset()
    vi.mocked(createUser).mockReset()
    vi.mocked(updateUserStatus).mockReset()
    vi.mocked(fetchUsers).mockResolvedValue(users)
    vi.mocked(createUser).mockResolvedValue({
      id: 2,
      account: 'developer',
      nickname: '开发人员',
      email: 'developer@example.com',
      avatarUrl: null,
      status: 'ACTIVE',
      role: 'MEMBER',
      createdAt: '2026-05-29T10:10:00',
      updatedAt: '2026-05-29T10:10:00',
    })
    vi.mocked(updateUserStatus).mockResolvedValue({
      ...users[0],
      status: 'DISABLED',
    })
  })

  it('loads users and creates a new account', async () => {
    const wrapper = mount(UserAdminView, {
      attachTo: document.body,
    })
    await flushPromises()

    expect(fetchUsers).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toContain('系统管理员')

    await wrapper.get('[aria-label="用户账号"]').setValue('developer')
    await wrapper.get('[aria-label="用户昵称"]').setValue('开发人员')
    await wrapper.get('[aria-label="用户邮箱"]').setValue('developer@example.com')
    await wrapper.get('[aria-label="初始密码"]').setValue('1')
    await wrapper.get('[aria-label="用户角色"]').setValue('MEMBER')
    await wrapper.get('form[aria-label="创建用户表单"]').trigger('submit')
    await flushPromises()

    expect(createUser).toHaveBeenCalledWith({
      account: 'developer',
      nickname: '开发人员',
      email: 'developer@example.com',
      password: '1',
      role: 'MEMBER',
    })
    expect(wrapper.text()).toContain('开发人员')
  })

  it('updates a user status from the list', async () => {
    const wrapper = mount(UserAdminView, {
      attachTo: document.body,
    })
    await flushPromises()

    await wrapper.get('[aria-label="停用 sd-robot"]').trigger('click')
    await flushPromises()

    expect(updateUserStatus).toHaveBeenCalledWith('sd-robot', 'DISABLED')
    expect(wrapper.text()).toContain('已停用')
  })
})
