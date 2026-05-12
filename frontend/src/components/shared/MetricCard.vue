<script setup lang="ts">
// 共享组件：封装可复用的界面块与交互单元。

defineProps<{
  label: string
  value: string | number
  sub?: string
}>()
</script>

<template>
  <div class="metric-card">
    <div class="metric-card__label">{{ label }}</div>
    <div class="metric-card__value">{{ value }}</div>
    <div v-if="sub" class="metric-card__sub">{{ sub }}</div>
  </div>
</template>
