<script setup lang="ts">
import { computed, ref } from 'vue'
import type { ProjectMember } from '../../api/projects'
import { searchActiveUsers, type UserDirectoryEntry } from '../../api/user-directory'

const props = defineProps<{
  members: ProjectMember[]
  ownerId: number
  currentUserId?: number | null
  loading?: boolean
  error?: string | null
}>()

const emit = defineEmits<{
  addMember: [userId: number]
  removeMember: [userId: number]
}>()

const keyword = ref('')
const candidates = ref<UserDirectoryEntry[]>([])
const searching = ref(false)
const searchError = ref<string | null>(null)

const memberIds = computed(() => new Set(props.members.map((member) => member.user.id)))
const availableCandidates = computed(() => (
  candidates.value.filter((candidate) => !memberIds.value.has(candidate.id))
))
function displayName(user: { nickname: string, account: string }) {
  return user.nickname ? `${user.nickname}（${user.account}）` : user.account
}

function roleLabel(member: ProjectMember) {
  return member.user.id === props.ownerId ? '负责人' : '成员'
}

function formatJoinedAt(joinedAt: string) {
  return joinedAt ? joinedAt.replace('T', ' ').slice(0, 16) : ''
}

async function searchUsers() {
  const trimmedKeyword = keyword.value.trim()
  searchError.value = null
  if (!trimmedKeyword) {
    candidates.value = []
    return
  }

  searching.value = true
  try {
    candidates.value = await searchActiveUsers(trimmedKeyword)
  } catch (error) {
    candidates.value = []
    searchError.value = '用户搜索失败'
  } finally {
    searching.value = false
  }
}
</script>

<template>
  <section class="panel-block project-member-manager" aria-label="项目成员管理">
    <div class="section-heading">
      <div>
        <h2>项目成员</h2>
        <span>{{ members.length }} 人</span>
      </div>
    </div>

    <form class="member-search-form" aria-label="搜索项目成员" @submit.prevent="searchUsers">
      <label>
        搜索用户
        <input
          v-model="keyword"
          aria-label="搜索用户"
          autocomplete="off"
          maxlength="80"
        />
      </label>
      <button class="secondary-button" type="submit" :disabled="searching || !keyword.trim()">
        {{ searching ? '搜索中' : '搜索' }}
      </button>
    </form>

    <p v-if="searchError" class="form-error">{{ searchError }}</p>
    <p v-else-if="!keyword.trim()" class="muted">输入昵称或账号后搜索可添加成员。</p>

    <div v-if="availableCandidates.length" class="member-candidates" aria-label="候选用户">
      <button
        v-for="candidate in availableCandidates"
        :key="candidate.id"
        class="candidate-button"
        type="button"
        :aria-label="`添加 ${candidate.nickname || candidate.account}`"
        @click="emit('addMember', candidate.id)"
      >
        <span>{{ displayName(candidate) }}</span>
        <small>添加</small>
      </button>
    </div>
    <p v-else-if="keyword.trim() && !searching && !searchError" class="muted">没有可添加的匹配用户。</p>

    <p v-if="loading" class="muted">正在加载成员...</p>
    <p v-if="error" class="form-error">{{ error }}</p>

    <ul class="member-list">
      <li v-for="member in members" :key="member.user.id" class="member-row">
        <span class="member-role" :class="{ owner: member.user.id === ownerId }">
          {{ roleLabel(member) }}
        </span>
        <span class="member-main">
          <strong>{{ member.user.nickname }}</strong>
          <small>{{ member.user.account }}</small>
        </span>
        <span class="member-joined">{{ formatJoinedAt(member.joinedAt) }}</span>
        <button
          v-if="member.user.id !== ownerId"
          class="danger-button"
          type="button"
          :aria-label="`移除 ${member.user.nickname || member.user.account}`"
          @click="emit('removeMember', member.user.id)"
        >
          移除
        </button>
      </li>
    </ul>
  </section>
</template>
