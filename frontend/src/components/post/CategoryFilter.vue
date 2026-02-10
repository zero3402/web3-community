<script setup lang="ts">
import type { Category } from '@/types'

defineProps<{
  categories: Category[]
  selectedId?: string
}>()

const emit = defineEmits<{
  select: [categoryId: string | undefined]
}>()
</script>

<template>
  <div class="category-filter">
    <button
      class="filter-btn"
      :class="{ active: !selectedId }"
      @click="emit('select', undefined)"
    >
      All
    </button>
    <button
      v-for="cat in categories"
      :key="cat.id"
      class="filter-btn"
      :class="{ active: selectedId === cat.id }"
      @click="emit('select', cat.id)"
    >
      {{ cat.name }}
    </button>
  </div>
</template>

<style scoped>
.category-filter {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 20px;
}

.filter-btn {
  padding: 6px 14px;
  border: 1px solid #d1d5db;
  background: white;
  border-radius: 20px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.filter-btn:hover {
  border-color: #3b82f6;
  color: #3b82f6;
}

.filter-btn.active {
  background-color: #3b82f6;
  color: white;
  border-color: #3b82f6;
}
</style>
