<script setup lang="ts">
import { ref } from 'vue'

defineProps<{
  parentId?: string
  placeholder?: string
}>()

const emit = defineEmits<{
  submit: [content: string]
  cancel: []
}>()

const content = ref('')

function handleSubmit() {
  if (content.value.trim()) {
    emit('submit', content.value.trim())
    content.value = ''
  }
}
</script>

<template>
  <div class="comment-form">
    <textarea
      v-model="content"
      :placeholder="placeholder || 'Write a comment...'"
      rows="3"
    ></textarea>
    <div class="comment-form-actions">
      <button v-if="parentId" class="btn btn-secondary" @click="emit('cancel')">Cancel</button>
      <button class="btn btn-primary" @click="handleSubmit" :disabled="!content.trim()">
        Submit
      </button>
    </div>
  </div>
</template>

<style scoped>
.comment-form textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 14px;
  resize: vertical;
}

.comment-form textarea:focus {
  outline: none;
  border-color: #3b82f6;
}

.comment-form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 8px;
}
</style>
