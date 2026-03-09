<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import api from '@/api/index'

const auth = useAuthStore()
const nickname = ref('')
const bio = ref('')
const loading = ref(false)
const message = ref('')

async function fetchProfile() {
  await auth.fetchUser()
  if (auth.user) {
    nickname.value = auth.user.nickname
    bio.value = auth.user.bio || ''
  }
}

async function handleUpdate() {
  loading.value = true
  message.value = ''
  try {
    const res = await api.put('/api/users/me', {
      nickname: nickname.value,
      bio: bio.value,
    })
    if (res.data.success) {
      message.value = 'Profile updated successfully'
      await auth.fetchUser()
    }
  } catch (e: any) {
    message.value = e.response?.data?.message || 'Failed to update profile'
  } finally {
    loading.value = false
  }
}

onMounted(fetchProfile)
</script>

<template>
  <div class="profile-page">
    <h1>My Profile</h1>
    <div class="card">
      <div class="profile-info" v-if="auth.user">
        <p><strong>Email:</strong> {{ auth.user.email }}</p>
        <p><strong>Role:</strong> {{ auth.user.role }}</p>
        <p><strong>Joined:</strong> {{ new Date(auth.user.createdAt).toLocaleDateString('ko-KR') }}</p>
      </div>

      <form @submit.prevent="handleUpdate">
        <div class="form-group">
          <label for="nickname">Nickname</label>
          <input id="nickname" v-model="nickname" type="text" required />
        </div>
        <div class="form-group">
          <label for="bio">Bio</label>
          <textarea id="bio" v-model="bio" rows="4" placeholder="Tell us about yourself..."></textarea>
        </div>
        <p v-if="message" :class="message.includes('success') ? 'success-message' : 'error-message'">
          {{ message }}
        </p>
        <button type="submit" class="btn btn-primary" :disabled="loading">
          {{ loading ? 'Updating...' : 'Update Profile' }}
        </button>
      </form>
    </div>
  </div>
</template>

<style scoped>
.profile-page h1 {
  margin-bottom: 20px;
}

.profile-info {
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e5e7eb;
}

.profile-info p {
  margin-bottom: 8px;
  font-size: 14px;
}

.success-message {
  color: #22c55e;
  font-size: 14px;
  margin-top: 4px;
  margin-bottom: 8px;
}
</style>
