<script setup lang="ts">
// 共享组件：封装可复用的界面块与交互单元。

defineProps<{
  title: string
  description?: string
}>()
</script>

<template>
  <div class="page-header">
    <div>
      <h2 class="page-title">{{ title }}</h2>
    </div>
    <slot />
  </div>
</template>
