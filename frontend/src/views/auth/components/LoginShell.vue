<script setup lang="ts">
// 共享组件：封装可复用的界面块与交互单元。

import { onMounted } from 'vue'
import { useSystemBranding } from '@/composables/useSystemBranding'

defineProps<{
  title: string
  subtitle: string
  badge?: string
  heading: string
  description: string
  highlights: Array<{ title: string; description: string }>
  variant?: 'admin' | 'client' | 'neutral'
}>()

const { siteName, loadBranding } = useSystemBranding()

onMounted(() => {
  void loadBranding().catch(() => undefined)
})
</script>

<template>
  <div class="login-shell" :class="variant ? `login-shell--${variant}` : undefined">
    <div class="login-shell__hero">
      <div class="login-shell__badge">{{ badge ?? siteName }}</div>
      <div class="login-shell__hero-main">
        <h1>{{ heading }}</h1>
        <p>{{ description }}</p>
      </div>
      <div class="login-shell__panel">
        <div v-for="item in highlights" :key="item.title">
          <strong>{{ item.title }}</strong>
          <span>{{ item.description }}</span>
        </div>
      </div>
    </div>

    <div class="login-shell__card">
      <div class="login-shell__top">
        <div class="login-shell__title">{{ title }}</div>
        <div class="login-shell__subtitle">{{ subtitle }}</div>
      </div>
      <slot />
    </div>
  </div>
</template>

<style scoped>
.login-shell {
  --login-accent: var(--accent);
  --login-hero: linear-gradient(145deg, rgba(76, 51, 27, 0.96), rgba(39, 25, 13, 0.98));
  --login-hero-glow: radial-gradient(circle at top left, rgba(215, 162, 85, 0.24), transparent 38%);

  display: grid;
  grid-template-columns: 1.15fr 0.85fr;
  min-height: 100vh;
  padding: 28px;
  gap: 24px;
  align-items: stretch;
  background:
    radial-gradient(circle at top, rgba(222, 207, 177, 0.35), transparent 35%),
    linear-gradient(180deg, #f5efe4 0%, #efe6d5 100%);
}

.login-shell--client {
  --login-accent: #3a79c8;
  --login-hero: linear-gradient(145deg, rgba(30, 51, 80, 0.95), rgba(18, 30, 47, 0.98));
  --login-hero-glow: radial-gradient(circle at top left, rgba(81, 164, 255, 0.22), transparent 40%);
}

.login-shell--neutral {
  --login-accent: #7b5832;
}

.login-shell__hero,
.login-shell__card {
  border: 1px solid rgba(117, 90, 53, 0.12);
  border-radius: 30px;
  box-shadow: 0 24px 60px rgba(71, 52, 28, 0.12);
}

.login-shell__hero {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 44px;
  background: var(--login-hero-glow), var(--login-hero);
  color: #f7f1e8;
  position: relative;
  overflow: hidden;
}

.login-shell__hero::after {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 18% 20%, rgba(255, 255, 255, 0.08), transparent 35%),
    radial-gradient(circle at 82% 18%, rgba(255, 255, 255, 0.06), transparent 30%),
    radial-gradient(circle at 70% 80%, rgba(0, 0, 0, 0.16), transparent 42%);
  pointer-events: none;
}

.login-shell__badge,
.login-shell__hero-main,
.login-shell__panel {
  position: relative;
  z-index: 1;
}

.login-shell__badge {
  display: inline-flex;
  width: fit-content;
  padding: 8px 14px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.06);
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.login-shell__hero h1 {
  max-width: 620px;
  margin: 24px 0 14px;
  font-size: 50px;
  line-height: 1.06;
  letter-spacing: -0.06em;
}

.login-shell__hero p {
  max-width: 620px;
  margin: 0;
  color: rgba(247, 241, 232, 0.76);
  font-size: 16px;
  line-height: 1.85;
}

.login-shell__panel {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-top: 30px;
}

.login-shell__panel > div {
  padding: 18px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.login-shell__panel strong,
.login-shell__panel span {
  display: block;
}

.login-shell__panel span {
  margin-top: 10px;
  color: rgba(247, 241, 232, 0.72);
  line-height: 1.7;
}

.login-shell__card {
  padding: 38px;
  background: rgba(255, 251, 245, 0.94);
  backdrop-filter: blur(16px);
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.login-shell__top {
  margin-bottom: 22px;
}

.login-shell__title {
  font-size: 30px;
  font-weight: 750;
  letter-spacing: -0.05em;
}

.login-shell__subtitle {
  margin-top: 10px;
  color: var(--muted);
  line-height: 1.7;
}

@media (max-width: 1024px) {
  .login-shell {
    grid-template-columns: 1fr;
  }

  .login-shell__hero {
    padding: 34px;
  }

  .login-shell__hero h1 {
    font-size: 38px;
  }
}

@media (max-width: 560px) {
  .login-shell {
    padding: 16px;
    gap: 16px;
  }

  .login-shell__card,
  .login-shell__hero {
    padding: 24px;
    border-radius: 24px;
  }

  .login-shell__panel {
    grid-template-columns: 1fr;
  }
}
</style>
