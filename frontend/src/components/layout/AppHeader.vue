<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'

const auth = useAuthStore()
const router = useRouter()

async function handleLogout() {
  await auth.logout()
  router.push('/login')
}
</script>

<template>
  <header class="header">
    <div class="container header-content">
      <router-link to="/" class="logo">Web3 Community</router-link>
      <nav class="nav">
        <template v-if="auth.isAuthenticated">
          <router-link to="/posts/write" class="nav-link">Write</router-link>
          <router-link to="/profile" class="nav-link">Profile</router-link>
          <button class="btn btn-secondary" @click="handleLogout">Logout</button>
        </template>
        <template v-else>
          <router-link to="/login" class="nav-link">Login</router-link>
          <router-link to="/register" class="btn btn-primary">Register</router-link>
        </template>
      </nav>
    </div>
  </header>
</template>

<style scoped>
.header {
  background: white;
  border-bottom: 1px solid #e5e7eb;
  padding: 12px 0;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.logo {
  font-size: 20px;
  font-weight: 700;
  color: #1f2937;
  text-decoration: none;
}

.nav {
  display: flex;
  align-items: center;
  gap: 16px;
}

.nav-link {
  color: #4b5563;
  font-size: 14px;
}

.nav-link:hover {
  color: #1f2937;
  text-decoration: none;
}
</style>
