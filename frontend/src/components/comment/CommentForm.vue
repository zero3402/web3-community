<template>
  <div class="comment-form">
    <div class="form-header">
      <div class="author-info">
        <el-avatar 
          :src="authStore.userAvatar" 
          :size="32"
          class="author-avatar"
        >
          <Icon icon="mdi:account" />
        </el-avatar>
        <div class="author-details">
          <span class="author-name">{{ authStore.userName }}</span>
          <span v-if="parentComment" class="reply-to">
            답글: {{ parentComment.author.username }}
          </span>
        </div>
      </div>
      
      <el-button 
        v-if="showCancel"
        size="small" 
        text 
        @click="$emit('cancel')"
      >
        취소
      </el-button>
    </div>

    <el-form
      ref="commentFormRef"
      :model="formData"
      :rules="formRules"
      @submit.prevent="handleSubmit"
      class="comment-form-element"
    >
      <el-form-item prop="content">
        <el-input
          v-model="formData.content"
          type="textarea"
          :rows="parentComment ? 3 : 4"
          :maxlength="maxContentLength"
          :placeholder="placeholder"
          show-word-limit
          resize="none"
          class="comment-textarea"
        />
      </el-form-item>

      <!-- 옵션 섹션 -->
      <div class="form-options">
        <!-- 이모지 추가 -->
        <el-popover placement="top-start" :width="300" trigger="click">
          <template #reference>
            <el-button size="small" text class="emoji-btn">
              <Icon icon="mdi:emoticon-happy" class="emoji-icon" />
            </el-button>
          </template>
          <div class="emoji-picker">
            <div class="emoji-categories">
              <button 
                v-for="category in emojiCategories" 
                :key="category.name"
                :class="['category-btn', { active: selectedCategory === category.name }]"
                @click="selectEmojiCategory(category.name)"
              >
                <Icon :icon="category.icon" class="category-icon" />
              </button>
            </div>
            <div class="emoji-grid">
              <button 
                v-for="emoji in selectedEmojis" 
                :key="emoji"
                class="emoji-item"
                @click="insertEmoji(emoji)"
              >
                {{ emoji }}
              </button>
            </div>
          </div>
        </el-popover>

        <!-- 파일 첨부 -->
        <el-upload
          ref="uploadRef"
          :auto-upload="false"
          :show-file-list="false"
          :on-change="handleFileChange"
          class="file-upload"
        >
          <el-button size="small" text class="upload-btn">
            <Icon icon="mdi:paperclip" class="upload-icon" />
            첨부파일
          </el-button>
        </el-upload>

        <!-- 제출 버튼 -->
        <el-button
          type="primary"
          size="small"
          :loading="isSubmitting"
          :disabled="!formData.content.trim()"
          @click="handleSubmit"
          class="submit-btn"
        >
          {{ parentComment ? '답글 등록' : '댓글 작성' }}
        </el-button>
      </div>

      <!-- 첨부된 파일 -->
      <div v-if="attachedFiles.length > 0" class="attached-files">
        <div 
          v-for="(file, index) in attachedFiles" 
          :key="index"
          class="attached-file"
        >
          <Icon icon="mdi:file" class="file-icon" />
          <span class="file-name">{{ file.name }}</span>
          <el-button 
            size="small" 
            text 
            @click="removeFile(index)"
            class="remove-btn"
          >
            <Icon icon="mdi:close" class="remove-icon" />
          </el-button>
        </div>
      </div>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules, type UploadProps } from 'element-plus'
import { Icon } from '@iconify/vue'
import { useAuthStore } from '@/stores/auth'

// =============================================================================
// 🎯 Props 정의
// =============================================================================
interface Props {
  postId: string
  parentComment?: any | null
  showCancel?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  parentComment: null,
  showCancel: false
})

// =============================================================================
// 🎯 Emits 정의
// =============================================================================
const emit = defineEmits<{
  submit: [data: { content: string; files: File[] }]
  cancel: []
}>()

// =============================================================================
// 🎯 컴포넌트 상태
// =============================================================================
const authStore = useAuthStore()

const commentFormRef = ref<FormInstance>()
const uploadRef = ref()

const formData = ref({
  content: ''
})

const isSubmitting = ref(false)
const attachedFiles = ref<File[]>([])
const selectedCategory = ref('smileys')

const maxContentLength = 1000

// =============================================================================
// 📋 폼 검증 규칙
// =============================================================================
const formRules: FormRules = {
  content: [
    { required: true, message: '댓글 내용을 입력해주세요.', trigger: 'blur' },
    { 
      min: 1, 
      message: '최소 1자 이상 입력해주세요.', 
      trigger: 'blur' 
    },
    { 
      max: maxContentLength, 
      message: `최대 ${maxContentLength}자까지 입력 가능합니다.`, 
      trigger: 'blur' 
    }
  ]
}

// =============================================================================
// 🎯 컴퓨티드 프로퍼티
// =============================================================================
const placeholder = computed(() => {
  if (props.parentComment) {
    return `${props.parentComment.author.username}님께 답글 작성...`
  }
  return '댓글을 작성해주세요...'
})

// =============================================================================
// 😀 이모지 피커
// =============================================================================
const emojiCategories = [
  { name: 'smileys', icon: 'mdi:emoticon-happy' },
  { name: 'people', icon: 'mdi:account-group' },
  { name: 'animals', icon: 'mdi:paw' },
  { name: 'food', icon: 'mdi:food' },
  { name: 'activities', icon: 'mdi:volleyball' },
  { name: 'travel', icon: 'mdi:airplane' },
  { name: 'objects', icon: 'mdi:cube-outline' },
  { name: 'symbols', icon: 'mdi:hashtag' },
  { name: 'flags', icon: 'mdi:flag' }
]

const emojis = {
  smileys: ['😀', '😃', '😄', '😁', '😆', '😅', '🤣', '😂', '🙂', '😊', '😇', '🙂‍↕️', '😉', '😌', '😍', '🥰', '😘', '😗', '😙', '😚', '😋', '😛', '😜', '🤪', '😝', '🤑', '🤗', '🤭', '🤫', '🤔', '😐', '😑', '😶', '😏', '😒', '🙄', '😬', '🤥', '😌', '😔', '😪', '🤤', '😴', '😷', '🤒', '🤕', '🤢', '🤮', '🤧', '🥵', '🥶', '😵', '🤯', '🤠', '🥳', '😎', '🤓', '🧐'],
  people: ['👋', '🤚', '🖐', '✋', '🖖', '👌', '🤌', '✌', '🤞', '🤟', '🤘', '🤙', '👈', '👉', '👆', '👇', '☝', '✋', '🤚', '🖖', '👍', '👎', '👊', '✊', '🤛', '🤜', '🤏', '👏', '🙌', '👐', '🤲', '🙏', '🤝', '💪', '🦾', '🦿', '🦵', '🦶', '👂', '👃', '👣', '👀', '👁', '👅', '👄', '💋', '👶', '🧒', '👦', '👧', '👱', '👨', '👩', '👴', '👵'],
  animals: ['🐶', '🐱', '🐭', '🐹', '🐰', '🦊', '🐻', '🐼', '🐨', '🐯', '🦁', '🐮', '🐷', '🐽', '🐸', '🐵', '🙈', '🙉', '🙊', '🐒', '🐔', '🐧', '🐦', '🐤', '🐣', '🐥', '🦆', '🦅', '🦉', '🦇', '🐺', '🐗', '🐴', '🦄', '🐛', '🦋', '🐌', '🐞', '🦗', '🦟', '🦗', '🦞', '🦂', '🕷', '🕸', '🦎', '🦖', '🦕', '🐍', '🐢', '🐙', '🦎', '🦑', '🦐', '🦀', '🐡', '🐠', '🐟', '🐬', '🐳'],
  food: ['🍏', '🍎', '🍐', '🍊', '🍋', '🍌', '🍉', '🍇', '🍓', '🍈', '🍒', '🍑', '🥭', '🍍', '🥥', '🥝', '🍅', '🍆', '🥑', '🥦', '🥬', '🥒', '🌶', '🌽', '🥕', '🥔', '🍠', '🥐', '🥚', '🧀', '🥓', '🥩', '🍗', '🍖', '🌭', '🍔', '🍟', '🍕', '🥪', '🥙', '🌮', '🍿', '🥡', '🥘', '🍜', '🍲', '🍛', '🍚', '🍙', '🥟', '🍤', '🍳', '🥞', '🥐', '🥨', '🧈', '🥞', '🍯'],
  activities: ['⚽', '🏀', '🏈', '⚾', '🥎', '🎾', '🏐', '🏉', '🎱', '🏓', '🏸', '🥌', '🏒', '🏑', '🥍', '🥏', '🎿', '🎼', '🎧', '🎹', '🎮', '🎰', '🧩', '🎳', '🎯', '🎲', '🎸', '🃏', '🀄', '🎴', '🎭', '🖌', '🖍', '🎨', '🧵', '🧶', '🪡', '🪡', '🪡', '🪡'],
  travel: ['🌍', '🌎', '🌏', '🌐', '🗺', '🗽', '🗾', '🌃', '🏙', '🌃', '🌄', '🌅', '🌆', '🌇', '🌉', '🌌', '🌠', '🎑', '🛣', '🛤', '🌉', '🗾', '🏔', '⛰', '🌋', '🗻', '🏕', '🏖', '🏜', '🏝', '🏞', '🏟', '🏛', '🏗', '🧱', '🪨', '🪵', '🛖', '🏘', '🏚', '🏠', '🏡', '🏢', '🏬', '🏣', '🏤', '🏥', '🏦', '🏨', '🏪', '🏫', '🏩', '🏪', '⛪', '🕌', '🛕', '🛤', '⛩'],
  objects: ['⌚', '⏰', '⏱', '⌛', '📔', '📕', '📖', '📗', '📘', '📙', '📚', '📓', '📔', '📒', '📜', '📃', '📄', '📰', '🗞', '📑', '🔖', '🏷', '💰', '💴', '💵', '💶', '💷', '💸', '💳', '🧾', '💎', '⚖', '🧰', '🔧', '🔨', '⚒', '🛠', '⛏', '🔩', '⚙', '🧱', '⛓', '🧲', '⚖', '🦯', '🔫', '💣', '🏹', '🔪', '🗡', '⚔', '🛡', '🚬', '⚰', '⚱', '🏺', '🎸', '🎹', '🥁', '🎷', '🎺'],
  symbols: ['❤️', '🧡', '💛', '💚', '💙', '💜', '🖤', '🤍', '🤎', '💔', '❣️', '💕', '💞', '💓', '💗', '💖', '💘', '💝', '☮', '🕉', '✡️', '☸️', '☯️', '✝️', '☦️', '☪️', '☕', '♾', '☯️', '☮', '🕉', '✝️', '☦️', '☪️', '☕', '♾', '☦', '♁', '♂', '⚥', '☤', '♆', '♇', '♈', '♉', '♊', '♋', '♌', '♍', '♎', '♏', '♐', '♑', '♒', '♓'],
  flags: ['🏁', '🚩', '🎌', '🏴', '🏳', '🏳️‍🌈', '🏴‍☠️', '🇦🇨', '🇦🇩', '🇦🇪', '🇦🇫', '🇦🇬', '🇦🇮', '🇦🇱', '🇦🇲', '🇦🇴', '🇦🇶', '🇦🇷', '🇦🇸', '🇦🇹', '🇦🇺', '🇦🇼', '🇦🇽', '🇦🇿', '🇧🇦', '🇧🇧', '🇧🇩', '🇧🇪', '🇧🇫', '🇧🇬', '🇧🇮', '🇧🇯']
}

const selectedEmojis = computed(() => {
  return emojis[selectedCategory.value as keyof typeof emojis] || []
})

// =============================================================================
// 🔧 유틸리티 함수
// =============================================================================
const selectEmojiCategory = (category: string): void => {
  selectedCategory.value = category
}

const insertEmoji = (emoji: string): void => {
  const textarea = document.querySelector('.comment-textarea textarea') as HTMLTextAreaElement
  if (textarea) {
    const start = textarea.selectionStart
    const end = textarea.selectionEnd
    const text = formData.value.content
    
    formData.value.content = text.substring(0, start) + emoji + text.substring(end)
    
    // 커서 위치 업데이트
    setTimeout(() => {
      textarea.selectionStart = textarea.selectionEnd = start + emoji.length
      textarea.focus()
    }, 0)
  }
}

const handleFileChange: UploadProps['onChange'] = (uploadFile) => {
  const file = uploadFile.raw
  
  // 파일 크기 체크 (10MB 제한)
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.error('파일 크기는 10MB를 초과할 수 없습니다.')
    return
  }
  
  // 파일 타입 체크
  const allowedTypes = ['image/jpeg', 'image/png', 'image/gif', 'application/pdf']
  if (!allowedTypes.includes(file.type)) {
    ElMessage.error('지원되지 않는 파일 형식입니다.')
    return
  }
  
  attachedFiles.value.push(file)
}

const removeFile = (index: number): void => {
  attachedFiles.value.splice(index, 1)
}

// =============================================================================
// 📝 폼 제출
// =============================================================================
const handleSubmit = async (): Promise<void> => {
  if (!commentFormRef.value) return

  try {
    const isValid = await commentFormRef.value.validate()
    if (!isValid) return

    isSubmitting.value = true

    const submitData = {
      content: formData.value.content.trim(),
      files: [...attachedFiles.value]
    }

    emit('submit', submitData)
    
    // 폼 초기화
    formData.value.content = ''
    attachedFiles.value = []
    
    ElMessage.success(props.parentComment ? '답글이 작성되었습니다.' : '댓글이 작성되었습니다.')

  } catch (error) {
    console.error('Failed to submit comment:', error)
    ElMessage.error('댓글 작성에 실패했습니다.')
  } finally {
    isSubmitting.value = false
  }
}

// =============================================================================
// 🎯 초기화
// =============================================================================
watch(() => props.parentComment, () => {
  formData.value.content = ''
  attachedFiles.value = []
})
</script>

<style scoped>
/* =============================================================================
// 🎨 댓글 폼 스타일
// ============================================================================= */
.comment-form {
  @apply bg-white dark:bg-gray-800 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700 p-4;
}

/* =============================================================================
// 📋 폼 헤더
// ============================================================================= */
.form-header {
  @apply flex items-center justify-between mb-4;
}

.author-info {
  @apply flex items-center gap-3;
}

.author-avatar {
  @apply border-2 border-gray-200 dark:border-gray-600;
}

.author-details {
  @apply flex flex-col;
}

.author-name {
  @apply font-medium text-gray-900 dark:text-gray-100;
}

.reply-to {
  @apply text-xs text-gray-500 dark:text-gray-400;
}

/* =============================================================================
// 📝 폼 요소
// ============================================================================= */
.comment-form-element {
  @apply space-y-3;
}

.comment-textarea :deep(.el-textarea__inner) {
  @apply border-gray-300 dark:border-gray-600 rounded-lg resize-none;
}

.comment-textarea :deep(.el-textarea__inner:focus) {
  @apply border-primary-500 dark:border-primary-400 ring-2 ring-primary-200 dark:ring-primary-800;
}

/* =============================================================================
// ⚙️ 폼 옵션
// ============================================================================= */
.form-options {
  @apply flex items-center justify-between flex-wrap gap-3;
}

.emoji-btn,
.upload-btn {
  @apply text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300 transition-colors duration-200;
}

.emoji-icon,
.upload-icon {
  @apply w-4 h-4;
}

.submit-btn {
  @apply font-medium;
}

/* =============================================================================
// 📎 첨부된 파일
// ============================================================================= */
.attached-files {
  @apply space-y-2;
}

.attached-file {
  @apply flex items-center gap-2 p-2 bg-gray-50 dark:bg-gray-700 rounded border border-gray-200 dark:border-gray-600;
}

.file-icon {
  @apply w-4 h-4 text-gray-400;
}

.file-name {
  @apply flex-1 text-sm text-gray-700 dark:text-gray-300 truncate;
}

.remove-btn {
  @apply text-gray-400 hover:text-red-500 transition-colors duration-200;
}

.remove-icon {
  @apply w-3 h-3;
}

/* =============================================================================
// 😀 이모지 피커
// ============================================================================= */
.emoji-picker {
  @apply p-3;
}

.emoji-categories {
  @apply flex gap-1 mb-3 pb-3 border-b border-gray-200 dark:border-gray-600;
}

.category-btn {
  @apply p-2 rounded text-gray-500 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors duration-200;
}

.category-btn.active {
  @apply text-primary-600 dark:text-primary-400 bg-primary-50 dark:bg-primary-900/20;
}

.category-icon {
  @apply w-4 h-4;
}

.emoji-grid {
  @apply grid grid-cols-8 gap-1 max-h-48 overflow-y-auto;
}

.emoji-item {
  @apply p-2 text-lg hover:bg-gray-100 dark:hover:bg-gray-700 rounded cursor-pointer transition-colors duration-200;
}

/* =============================================================================
// 📱 반응형 디자인
// ============================================================================= */
@media (max-width: 640px) {
  .comment-form {
    @apply p-3;
  }
  
  .form-options {
    @apply flex-col items-stretch;
  }
  
  .emoji-grid {
    @apply grid-cols-6;
  }
  
  .attached-file {
    @apply p-1;
  }
}
</style>