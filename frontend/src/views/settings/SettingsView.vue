<script setup lang="ts">
// 页面文件：负责组织当前页面的数据加载、交互行为和展示内容。

import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { systemConfigApi } from '@/api/modules/management'
import type { RechargeBonusTierRecord, SystemConfigRecord } from '@/types/business'
import PageIntro from '@/components/shared/PageIntro.vue'
import { useSystemBranding } from '@/composables/useSystemBranding'

const loading = ref(false)
const savingPolicy = ref(false)
const savingBasic = ref(false)
const configs = ref<SystemConfigRecord[]>([])
const { refreshBranding, setBranding } = useSystemBranding()

const policyForm = reactive({
  enabled: true,
  tiers: [
    { minAmount: 100, bonusAmount: 10 },
    { minAmount: 300, bonusAmount: 40 },
  ] as RechargeBonusTierRecord[],
})

const basicForm = reactive({
  siteName: '茶室平台',
  customerServicePhone: '400-000-0000',
})

const sortedTiers = computed(() => [...policyForm.tiers].sort((a, b) => a.minAmount - b.minAmount))

function configValueOf(key: string) {
  return configs.value.find((item) => item.configKey === key)?.configValue
}

function addTier() {
  policyForm.tiers.push({ minAmount: 100, bonusAmount: 10 })
}

function removeTier(index: number) {
  policyForm.tiers.splice(index, 1)
}

async function loadSettings() {
  loading.value = true
  try {
    const page = await systemConfigApi.page({ page: 0, size: 100 })
    configs.value = page.content ?? []

    basicForm.siteName = configValueOf('site_name') || '茶室平台'
    basicForm.customerServicePhone = configValueOf('customer_service_phone') || '400-000-0000'

    const enabledConfig = configValueOf('membership_recharge_enabled')
    const tierConfig = configValueOf('membership_recharge_bonus_rules')

    policyForm.enabled = enabledConfig ? enabledConfig === 'true' : true
    if (tierConfig) {
      try {
        const parsed = JSON.parse(tierConfig) as RechargeBonusTierRecord[]
        policyForm.tiers = parsed
          .filter((item) => Number(item.minAmount) > 0 && Number(item.bonusAmount) >= 0)
          .map((item) => ({
            minAmount: Number(item.minAmount),
            bonusAmount: Number(item.bonusAmount),
          }))
        if (policyForm.tiers.length === 0) addTier()
      } catch {
        policyForm.tiers = [{ minAmount: 100, bonusAmount: 10 }]
      }
    }
  } finally {
    loading.value = false
  }
}

async function saveBasicSettings() {
  savingBasic.value = true
  try {
    const siteName = basicForm.siteName.trim() || '茶室平台'
    const customerServicePhone = basicForm.customerServicePhone.trim() || '400-000-0000'

    await Promise.all([
      systemConfigApi.save({
        configKey: 'site_name',
        configValue: siteName,
        description: '平台名称',
      }),
      systemConfigApi.save({
        configKey: 'customer_service_phone',
        configValue: customerServicePhone,
        description: '客服电话',
      }),
    ])

    setBranding({ siteName, customerServicePhone })
    await refreshBranding().catch(() => undefined)
    ElMessage.success('基础业务设置已更新')
    await loadSettings()
  } finally {
    savingBasic.value = false
  }
}

async function saveRechargePolicy() {
  const normalized = sortedTiers.value.filter((item) => item.minAmount > 0 && item.bonusAmount >= 0)
  if (normalized.length === 0) {
    ElMessage.warning('请至少保留一条充值优惠规则')
    return
  }

  savingPolicy.value = true
  try {
    await Promise.all([
      systemConfigApi.save({
        configKey: 'membership_recharge_enabled',
        configValue: String(policyForm.enabled),
        description: '是否开启会员充值',
      }),
      systemConfigApi.save({
        configKey: 'membership_recharge_bonus_rules',
        configValue: JSON.stringify(normalized),
        description: '会员充值优惠规则',
      }),
    ])
    policyForm.tiers = normalized
    ElMessage.success('充值优惠已更新')
    await loadSettings()
  } finally {
    savingPolicy.value = false
  }
}

onMounted(loadSettings)
</script>

<template>
  <div class="page-shell">
    <PageIntro title="系统设置" description="这里展示给业务管理员可直接理解和使用的业务设置，不暴露底层技术配置键和值。" />

    <div class="settings-grid">
      <div class="section-card" v-loading="loading">
        <div class="section-card__header">
          <h3 class="section-card__title">基础业务设置</h3>
        </div>
        <div class="section-card__body">
          <div class="simple-form">
            <label class="field">
              <span class="field__label">平台名称</span>
              <el-input v-model="basicForm.siteName" maxlength="50" />
              <span class="field__hint">用于页面标题、平台展示名称等业务场景。</span>
            </label>

            <label class="field">
              <span class="field__label">客服电话</span>
              <el-input v-model="basicForm.customerServicePhone" maxlength="30" />
              <span class="field__hint">用于用户联系平台客服。</span>
            </label>
          </div>

          <div class="policy-actions">
            <el-button type="primary" :loading="savingBasic" @click="saveBasicSettings">保存基础设置</el-button>
          </div>
        </div>
      </div>

      <div class="section-card" v-loading="loading">
        <div class="section-card__header">
          <h3 class="section-card__title">会员充值优惠</h3>
        </div>
        <div class="section-card__body">
          <div class="policy-head">
            <div>
              <div class="policy-title">会员充值开关</div>
              <div class="policy-sub">关闭后，前台用户将不能使用会员充值入口。</div>
            </div>
            <el-switch v-model="policyForm.enabled" />
          </div>

          <div class="tier-editor">
            <div v-for="(tier, index) in policyForm.tiers" :key="index" class="tier-editor__row">
              <el-input-number v-model="tier.minAmount" :min="0.01" :precision="2" />
              <span class="tier-editor__label">充值满</span>
              <el-input-number v-model="tier.bonusAmount" :min="0" :precision="2" />
              <span class="tier-editor__label">赠送</span>
              <el-button text type="danger" @click="removeTier(index)">删除</el-button>
            </div>
          </div>

          <div class="policy-actions">
            <el-button plain @click="addTier">新增档位</el-button>
            <el-button type="primary" :loading="savingPolicy" @click="saveRechargePolicy">保存充值规则</el-button>
          </div>

          <div class="preview-grid">
            <div v-for="tier in sortedTiers" :key="`${tier.minAmount}-${tier.bonusAmount}`" class="preview-card">
              <strong>充 ¥{{ tier.minAmount }}</strong>
              <span>赠 ¥{{ tier.bonusAmount }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.settings-grid {
  display: grid;
  gap: 20px;
}

.simple-form {
  display: grid;
  gap: 18px;
}

.field {
  display: grid;
  gap: 8px;
}

.field__label {
  font-weight: 700;
  color: #264235;
}

.field__hint {
  color: var(--muted);
  line-height: 1.7;
  font-size: 13px;
}

.policy-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 20px;
  border-radius: 20px;
  background: linear-gradient(145deg, rgba(248, 244, 236, 0.95), rgba(255, 255, 255, 0.96));
}

.policy-title {
  font-size: 18px;
  font-weight: 700;
  color: #264235;
}

.policy-sub {
  margin-top: 6px;
  color: var(--muted);
  line-height: 1.7;
}

.tier-editor {
  display: grid;
  gap: 14px;
  margin-top: 18px;
}

.tier-editor__row {
  display: grid;
  grid-template-columns: 140px auto 140px auto auto;
  gap: 10px;
  align-items: center;
}

.tier-editor__label {
  color: var(--muted);
}

.policy-actions {
  display: flex;
  gap: 12px;
  margin-top: 18px;
}

.preview-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 14px;
  margin-top: 20px;
}

.preview-card {
  padding: 18px;
  border-radius: 18px;
  background: rgba(47, 93, 71, 0.06);
}

.preview-card strong,
.preview-card span {
  display: block;
}

.preview-card strong {
  color: #264235;
  font-size: 18px;
}

.preview-card span {
  margin-top: 8px;
  color: #8a5a2b;
  font-weight: 700;
}

@media (max-width: 900px) {
  .tier-editor__row {
    grid-template-columns: 1fr 1fr;
  }

  .tier-editor__label {
    display: none;
  }

  .policy-actions {
    flex-direction: column;
  }
}
</style>
