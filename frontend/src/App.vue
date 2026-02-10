<script setup lang="ts">
import AppHeader from '@/components/layout/AppHeader.vue'
import AppFooter from '@/components/layout/AppFooter.vue'
</script>

<template>
  <div id="app-wrapper">
    <AppHeader />
    <main class="main-content container">
      <router-view />
    </main>
    <AppFooter />
  </div>
</template>

<style scoped>
#app-wrapper {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.main-content {
  flex: 1;
  padding-top: 24px;
  padding-bottom: 40px;
}
</style>
