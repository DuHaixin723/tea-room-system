<script setup lang="ts">
// 页面文件：负责组织当前页面的数据加载、交互行为和展示内容。

import { computed, onMounted, reactive, ref, watch } from 'vue'
import dayjs from 'dayjs'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { couponApi, systemConfigApi, usersApi } from '@/api/modules/management'
import { useAuthStore } from '@/stores/auth'
import { useMemberStore } from '@/stores/member'
import type { MemberAccountRecord, MemberRechargeRecord, RechargeBonusTierRecord, RechargePolicyRecord, UserCouponRecord } from '@/types/business'
import PageIntro from '@/components/shared/PageIntro.vue'

interface RechargeForm {
  amount: number
  couponId: number | null
}

interface MemberLevelRule {
  code: string
  label: string
  recharge: number
  spend: number
  color: string
}

const LEVEL_RULES: MemberLevelRule[] = [
  { code: 'NORMAL', label: '普通会员', recharge: 0, spend: 0, color: '#8b6b4c' },
  { code: 'SILVER', label: '白银会员', recharge: 300, spend: 500, color: '#7c8b9a' },
  { code: 'GOLD', label: '黄金会员', recharge: 1000, spend: 2000, color: '#b78831' },
  { code: 'PLATINUM', label: '铂金会员', recharge: 3000, spend: 5000, color: '#3f6c74' },
  { code: 'DIAMOND', label: '钻石会员', recharge: 8000, spend: 12000, color: '#28618c' },
]

const authStore = useAuthStore()
const memberStore = useMemberStore()
const route = useRoute()
const router = useRouter()
const rechargeFormRef = ref<FormInstance>()
const account = ref<MemberAccountRecord | null>(null)
const rechargeRecords = ref<MemberRechargeRecord[]>([])
const policy = ref<RechargePolicyRecord>({ enabled: true, tiers: [] })
const coupons = ref<UserCouponRecord[]>([])
const recharging = ref(false)

const rechargeForm = reactive<RechargeForm>({
  amount: Number(route.query.amount ?? 100),
  couponId: null,
})

const rechargeRules: FormRules<RechargeForm> = {
  amount: [{ required: true, message: '请输入充值金额', trigger: 'change' }],
}

const matchedTier = computed<RechargeBonusTierRecord | null>(() => {
  const amount = Number(rechargeForm.amount ?? 0)
  return [...policy.value.tiers].sort((a, b) => a.minAmount - b.minAmount).filter((item) => amount >= item.minAmount).at(-1) ?? null
})

const sortedTiers = computed(() => [...policy.value.tiers].sort((a, b) => a.minAmount - b.minAmount))
const bonusAmount = computed(() => matchedTier.value?.bonusAmount ?? 0)
const creditedAmount = computed(() => Number(rechargeForm.amount ?? 0) + bonusAmount.value)
const nextBonusTier = computed(() => {
  const amount = Number(rechargeForm.amount ?? 0)
  return sortedTiers.value.find((item) => amount < item.minAmount) ?? null
})
const nextBonusGap = computed(() => {
  if (!nextBonusTier.value) return 0
  return Math.max(nextBonusTier.value.minAmount - Number(rechargeForm.amount ?? 0), 0)
})
const customRechargeHint = computed(() => {
  const amount = Number(rechargeForm.amount ?? 0)
  if (amount <= 0) return '输入任意充值金额后，系统会自动匹配当前可用的充值赠送档位。'
  if (matchedTier.value) {
    const base = `当前金额已命中“充 ¥${matchedTier.value.minAmount} 赠 ¥${matchedTier.value.bonusAmount}”优惠`
    if (nextBonusTier.value) {
      return `${base}，再充 ¥${nextBonusGap.value.toFixed(2)} 可升级到下一档优惠。`
    }
    return `${base}，当前已是最高优惠档位。`
  }
  if (nextBonusTier.value) {
    return `当前金额暂未命中优惠，再充 ¥${nextBonusGap.value.toFixed(2)} 可享“充 ¥${nextBonusTier.value.minAmount} 赠 ¥${nextBonusTier.value.bonusAmount}”。`
  }
  return '当前没有可用的充值优惠档位。'
})
const returnTarget = computed(() => String(route.query.redirect ?? '/orders'))
const fromOrderNo = computed(() => String(route.query.orderNo ?? ''))

const currentLevelRule = computed(() => {
  const code = account.value?.memberLevel ?? 'NORMAL'
  return LEVEL_RULES.find((item) => item.code === code) ?? LEVEL_RULES[0]
})

const nextLevelRule = computed(() => {
  const index = LEVEL_RULES.findIndex((item) => item.code === currentLevelRule.value.code)
  if (index < 0 || index === LEVEL_RULES.length - 1) return null
  return LEVEL_RULES[index + 1]
})

const levelProgress = computed(() => {
  if (!account.value || !nextLevelRule.value) return 100
  const currentIndex = LEVEL_RULES.findIndex((item) => item.code === currentLevelRule.value.code)
  const currentBase = LEVEL_RULES[Math.max(0, currentIndex)].recharge
  const nextBase = nextLevelRule.value.recharge
  const currentRecharge = account.value.cumulativeRecharge + Number(rechargeForm.amount || 0)
  if (nextBase <= currentBase) return 100
  return Math.max(0, Math.min(100, ((currentRecharge - currentBase) / (nextBase - currentBase)) * 100))
})

const projectedLevelLabel = computed(() => {
  const projectedRecharge = (account.value?.cumulativeRecharge ?? 0) + Number(rechargeForm.amount || 0)
  const projectedSpend = account.value?.cumulativeSpend ?? 0
  const matched = [...LEVEL_RULES].reverse().find((item) => projectedRecharge >= item.recharge || projectedSpend >= item.spend) ?? LEVEL_RULES[0]
  return matched.label
})

const nextLevelGap = computed(() => {
  if (!account.value || !nextLevelRule.value) return 0
  return Math.max(nextLevelRule.value.recharge - account.value.cumulativeRecharge, 0)
})

const availableCoupons = computed(() =>
  coupons.value.filter((item) => item.status === 'UNUSED' && dayjs(item.validFrom).isBefore(dayjs()) && dayjs(item.validUntil).isAfter(dayjs())),
)
const usableCoupons = computed(() => {
  const amount = Number(rechargeForm.amount ?? 0)
  return availableCoupons.value
    .filter((item) => amount >= Number(item.thresholdAmount))
    .sort((a, b) => Number(b.discountAmount) - Number(a.discountAmount))
})
const selectedCoupon = computed(() => usableCoupons.value.find((item) => item.id === rechargeForm.couponId) ?? null)
const couponDiscount = computed(() => Number(selectedCoupon.value?.discountAmount ?? 0))
const actualPayment = computed(() => Math.max(Number(rechargeForm.amount ?? 0) - couponDiscount.value, 0))
const expiringSoonCoupons = computed(() => availableCoupons.value.filter((item) => dayjs(item.validUntil).diff(dayjs(), 'hour') <= 24))
const expiredCoupons = computed(() => coupons.value.filter((item) => item.status === 'EXPIRED').slice(0, 3))
const recommendedCoupon = computed(() => {
  return usableCoupons.value[0] ?? null
})

function selectTier(tier: RechargeBonusTierRecord) {
  rechargeForm.amount = tier.minAmount
}

function selectCoupon(couponId: number | null) {
  rechargeForm.couponId = couponId
}

function levelLabel(level?: string) {
  return LEVEL_RULES.find((item) => item.code === level)?.label ?? '普通会员'
}

function couponStatusType(status: UserCouponRecord['status']) {
  if (status === 'UNUSED') return 'success'
  if (status === 'USED') return 'info'
  return 'danger'
}

function couponStatusLabel(status: UserCouponRecord['status']) {
  if (status === 'UNUSED') return '可使用'
  if (status === 'USED') return '已使用'
  return '已过期'
}

function couponCountdown(coupon: UserCouponRecord) {
  if (coupon.status === 'EXPIRED') return '已过期'
  const diffHours = dayjs(coupon.validUntil).diff(dayjs(), 'hour')
  if (diffHours <= 24) return `即将到期，还剩 ${Math.max(diffHours, 0)} 小时`
  return `到期时间 ${dayjs(coupon.validUntil).format('MM-DD HH:mm')}`
}

watch(
  () => [rechargeForm.amount, usableCoupons.value.map((item) => item.id).join(',')] as const,
  () => {
    if (rechargeForm.couponId && !usableCoupons.value.some((item) => item.id === rechargeForm.couponId)) {
      rechargeForm.couponId = null
    }
  },
  { immediate: true },
)

async function loadAccount() {
  if (!authStore.user) return
  const [accountData, rechargeData, policyData, couponData] = await Promise.all([
    usersApi.account(authStore.user.id),
    usersApi.rechargeRecords(authStore.user.id),
    systemConfigApi.rechargePolicy(),
    couponApi.page({ page: 0, size: 50 }),
  ])
  account.value = accountData
  memberStore.setAccount(accountData)
  rechargeRecords.value = rechargeData
  policy.value = policyData
  coupons.value = couponData.content
}

async function recharge() {
  const valid = await rechargeFormRef.value?.validate().catch(() => false)
  if (!valid || !authStore.user) return
  if (!policy.value.enabled) {
    ElMessage.warning('当前暂未开放会员充值')
    return
  }

  recharging.value = true
  try {
    account.value = await usersApi.recharge(authStore.user.id, rechargeForm)
    memberStore.setAccount(account.value)
    rechargeRecords.value = await usersApi.rechargeRecords(authStore.user.id)
    const couponData = await couponApi.page({ page: 0, size: 50 })
    coupons.value = couponData.content
    ElMessage.success(
      bonusAmount.value > 0
        ? `充值成功，已额外赠送 ¥${bonusAmount.value}，当前等级 ${levelLabel(account.value.memberLevel)}`
        : `充值成功，当前等级 ${levelLabel(account.value.memberLevel)}`,
    )
  } finally {
    recharging.value = false
  }
}

function goBackToPay() {
  router.push(returnTarget.value)
}

function goToCoupons() {
  router.push('/coupons')
}

onMounted(loadAccount)
</script>

<template>
  <div class="page-shell">
    <PageIntro
      title="账户充值与会员成长"
      description="充值金额会进入会员余额，系统会自动匹配充值赠送档位。会员优惠券也会在本页同步展示，到期前后都会提醒。"
    />

    <div class="recharge-layout">
      <div class="section-card hero-card">
        <div class="section-card__body hero-card__body">
          <div class="hero-copy">
            <div class="hero-eyebrow">会员成长计划</div>
            <h2>充值、消费、等级权益联动展示</h2>
            <p>你可以通过充值增长储值账户，也可以通过消费提升会员等级。当前页面会同步展示账户金额、成长进度、充值优惠和可用优惠券。</p>

            <div class="growth-panel">
              <div class="growth-panel__head">
                <div>
                  <span>当前等级</span>
                  <strong :style="{ color: currentLevelRule.color }">{{ currentLevelRule.label }}</strong>
                </div>
                <div>
                  <span>预计充值后</span>
                  <strong>{{ projectedLevelLabel }}</strong>
                </div>
              </div>
              <el-progress :percentage="Number(levelProgress.toFixed(1))" :stroke-width="14" :show-text="false" />
              <div class="growth-panel__foot">
                <span v-if="nextLevelRule">距离 {{ nextLevelRule.label }} 还差累计充值 ¥{{ nextLevelGap.toFixed(2) }}</span>
                <span v-else>当前已经是最高等级</span>
              </div>
            </div>
          </div>

          <div class="hero-metrics">
            <div class="metric-card">
              <div class="metric-card__label">当前余额</div>
              <div class="metric-card__value">¥{{ account?.balance ?? 0 }}</div>
              <div class="metric-card__sub">会员等级：{{ levelLabel(account?.memberLevel) }}</div>
            </div>
            <div class="metric-card">
              <div class="metric-card__label">累计充值</div>
              <div class="metric-card__value">¥{{ account?.cumulativeRecharge ?? 0 }}</div>
              <div class="metric-card__sub">充值达到门槛即可升级</div>
            </div>
            <div class="metric-card is-highlight">
              <div class="metric-card__label">累计消费</div>
              <div class="metric-card__value">¥{{ account?.cumulativeSpend ?? 0 }}</div>
              <div class="metric-card__sub">消费满额同样可以提升等级</div>
            </div>
          </div>
        </div>
      </div>

      <div class="recharge-main-grid">
        <div class="section-card">
          <div class="section-card__header">
            <h3 class="section-card__title">充值优惠</h3>
          </div>
          <div class="section-card__body">
            <div v-if="policy.enabled" class="tier-grid">
              <button
                v-for="tier in policy.tiers"
                :key="`${tier.minAmount}-${tier.bonusAmount}`"
                type="button"
                class="tier-card"
                :class="{ 'is-active': matchedTier?.minAmount === tier.minAmount && Number(rechargeForm.amount) === tier.minAmount }"
                @click="selectTier(tier)"
              >
                <strong>充 ¥{{ tier.minAmount }}</strong>
                <span>赠 ¥{{ tier.bonusAmount }}</span>
              </button>
            </div>
            <el-empty v-else description="当前暂未开放充值" />

            <el-form ref="rechargeFormRef" :model="rechargeForm" :rules="rechargeRules" label-position="top" class="recharge-form">
              <el-form-item label="充值金额" prop="amount">
                <el-input-number v-model="rechargeForm.amount" :min="0.01" :precision="2" style="width: 100%" />
              </el-form-item>
              <el-form-item label="优惠券抵扣">
                <el-select
                  v-model="rechargeForm.couponId"
                  clearable
                  placeholder="不使用优惠券"
                  style="width: 100%"
                  @clear="selectCoupon(null)"
                >
                  <el-option
                    v-for="coupon in usableCoupons"
                    :key="coupon.id"
                    :label="`${coupon.title} | 满¥${coupon.thresholdAmount} 减¥${coupon.discountAmount}`"
                    :value="coupon.id"
                  />
                </el-select>
              </el-form-item>
              <div class="arrival-tip">
                <span>预计到账</span>
                <strong>¥{{ creditedAmount.toFixed(2) }}</strong>
                <em v-if="bonusAmount > 0">含赠送 ¥{{ bonusAmount.toFixed(2) }}</em>
              </div>
              <div class="arrival-tip payment-tip">
                <span>实付金额</span>
                <strong>¥{{ actualPayment.toFixed(2) }}</strong>
                <em v-if="selectedCoupon">已使用 {{ selectedCoupon.title }}，抵扣 ¥{{ couponDiscount.toFixed(2) }}</em>
              </div>
              <div class="bonus-tip" :class="{ 'is-hit': bonusAmount > 0 }">
                {{ customRechargeHint }}
              </div>
              <div v-if="recommendedCoupon" class="bonus-tip coupon-hit">
                当前充值金额可参考 {{ recommendedCoupon.title }}，满 ¥{{ recommendedCoupon.thresholdAmount }} 减 ¥{{ recommendedCoupon.discountAmount }}。
              </div>
              <div v-if="usableCoupons.length" class="coupon-picks">
                <button
                  v-for="coupon in usableCoupons.slice(0, 3)"
                  :key="coupon.id"
                  type="button"
                  class="coupon-pick"
                  :class="{ 'is-active': rechargeForm.couponId === coupon.id }"
                  @click="selectCoupon(rechargeForm.couponId === coupon.id ? null : coupon.id)"
                >
                  <strong>{{ coupon.title }}</strong>
                  <span>满¥{{ coupon.thresholdAmount }} 减¥{{ coupon.discountAmount }}</span>
                </button>
              </div>
              <div class="actions">
                <el-button v-if="route.query.redirect" plain @click="goBackToPay">返回继续支付</el-button>
                <el-button type="primary" :disabled="!policy.enabled" :loading="recharging" @click="recharge">立即充值</el-button>
              </div>
              <div v-if="fromOrderNo" class="return-tip">
                当前是为订单 {{ fromOrderNo }} 补充余额，充值完成后可直接返回继续支付。
              </div>
            </el-form>
          </div>
        </div>

        <div class="section-card level-card">
          <div class="section-card__header">
            <h3 class="section-card__title">会员等级说明</h3>
          </div>
          <div class="section-card__body">
            <div v-for="level in LEVEL_RULES" :key="level.code" class="level-row" :class="{ 'is-current': account?.memberLevel === level.code }">
              <div>
                <strong :style="{ color: level.color }">{{ level.label }}</strong>
                <span>{{ level.code }}</span>
              </div>
              <div class="level-row__meta">
                <span>累计充值满 ¥{{ level.recharge }}</span>
                <span>或累计消费满 ¥{{ level.spend }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="section-card">
        <div class="section-card__header coupon-header">
          <div>
            <h3 class="section-card__title">我的优惠券</h3>
            <p>充值页可直接查看优惠券详情、到期时间和即将过期提醒。</p>
          </div>
          <el-button plain @click="goToCoupons">查看全部</el-button>
        </div>
        <div class="section-card__body">
          <el-alert
            v-if="expiringSoonCoupons.length"
            type="warning"
            show-icon
            :closable="false"
            :title="`有 ${expiringSoonCoupons.length} 张优惠券将在 24 小时内到期，请尽快使用。`"
          />
          <el-alert
            v-if="expiredCoupons.length"
            class="coupon-alert"
            type="error"
            show-icon
            :closable="false"
            :title="`有 ${expiredCoupons.length} 张优惠券已过期，可在通知中心查看提醒。`"
          />

          <div v-if="coupons.length" class="coupon-list">
            <article v-for="coupon in coupons.slice(0, 6)" :key="coupon.id" class="coupon-item" :class="`is-${coupon.status.toLowerCase()}`">
              <div class="coupon-item__head">
                <div>
                  <h4>{{ coupon.title }}</h4>
                  <p>{{ levelLabel(coupon.sourceLevel) }}专享，满 ¥{{ coupon.thresholdAmount }} 减 ¥{{ coupon.discountAmount }}</p>
                </div>
                <el-tag :type="couponStatusType(coupon.status)">{{ couponStatusLabel(coupon.status) }}</el-tag>
              </div>
              <div class="coupon-item__meta">
                <span>券码：{{ coupon.couponCode }}</span>
                <span>{{ couponCountdown(coupon) }}</span>
              </div>
              <div class="coupon-item__meta">
                <span>有效期：{{ dayjs(coupon.validFrom).format('YYYY-MM-DD HH:mm') }}</span>
                <span>至 {{ dayjs(coupon.validUntil).format('YYYY-MM-DD HH:mm') }}</span>
              </div>
            </article>
          </div>
          <el-empty v-else description="当前暂无优惠券" />
        </div>
      </div>

      <div class="section-card">
        <div class="section-card__header">
          <h3 class="section-card__title">最近充值记录</h3>
        </div>
        <div class="section-card__body">
          <el-table :data="rechargeRecords.slice(0, 10)" empty-text="暂无充值记录">
            <el-table-column prop="createdAt" label="时间" min-width="180" />
            <el-table-column prop="amount" label="实付金额" width="120">
              <template #default="{ row }">¥{{ row.amount }}</template>
            </el-table-column>
            <el-table-column prop="balanceAfter" label="充值后余额" width="140">
              <template #default="{ row }">¥{{ row.balanceAfter }}</template>
            </el-table-column>
            <el-table-column prop="remark" label="说明" min-width="320" />
          </el-table>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.recharge-layout {
  display: grid;
  gap: 20px;
}

.hero-card__body {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(360px, 460px);
  gap: 24px;
  align-items: stretch;
}

.hero-card {
  overflow: hidden;
}

.hero-copy {
  padding: 8px 0;
}

.hero-copy h2 {
  margin: 10px 0 12px;
  font-size: 34px;
  line-height: 1.15;
  color: #264235;
}

.hero-copy p {
  max-width: 640px;
  color: var(--muted);
  line-height: 1.8;
}

.hero-eyebrow {
  display: inline-flex;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(47, 93, 71, 0.08);
  color: #2f5d47;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.growth-panel {
  margin-top: 22px;
  padding: 20px;
  border-radius: 24px;
  background: linear-gradient(145deg, rgba(255, 250, 242, 0.95), rgba(244, 250, 245, 0.96));
}

.growth-panel__head,
.growth-panel__foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.growth-panel__head span,
.growth-panel__foot span {
  color: var(--muted);
  font-size: 13px;
}

.growth-panel__head strong {
  display: block;
  margin-top: 8px;
  font-size: 24px;
}

.growth-panel :deep(.el-progress) {
  margin-top: 16px;
}

.growth-panel__foot {
  margin-top: 12px;
}

.hero-metrics {
  display: grid;
  gap: 14px;
}

.metric-card {
  padding: 20px;
  border-radius: 22px;
  background: linear-gradient(145deg, rgba(248, 244, 236, 0.95), rgba(255, 255, 255, 0.96));
}

.metric-card.is-highlight {
  background: linear-gradient(145deg, rgba(47, 93, 71, 0.1), rgba(255, 245, 230, 0.92));
}

.metric-card__label {
  color: var(--muted);
  font-size: 13px;
}

.metric-card__value {
  margin-top: 10px;
  font-size: 30px;
  font-weight: 800;
  color: #2f5d47;
}

.metric-card__sub {
  margin-top: 8px;
  color: var(--muted);
}

.recharge-main-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(320px, 420px);
  gap: 20px;
}

.tier-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 14px;
}

.tier-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 18px;
  border: 1px solid rgba(38, 66, 53, 0.1);
  border-radius: 20px;
  background: #fff;
  text-align: left;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.tier-card:hover,
.tier-card.is-active {
  border-color: rgba(47, 93, 71, 0.45);
  box-shadow: 0 16px 36px rgba(47, 93, 71, 0.12);
  transform: translateY(-2px);
}

.tier-card strong {
  font-size: 18px;
  color: #22352b;
}

.tier-card span {
  color: #8a5a2b;
  font-weight: 700;
}

.recharge-form {
  margin-top: 22px;
}

.arrival-tip {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin: 4px 0 18px;
  color: var(--muted);
}

.arrival-tip strong {
  font-size: 28px;
  color: #2f5d47;
}

.arrival-tip em {
  font-style: normal;
  color: #8a5a2b;
}

.payment-tip strong {
  color: #7a4f22;
}

.bonus-tip {
  margin: -4px 0 18px;
  padding: 12px 14px;
  border-radius: 16px;
  background: rgba(47, 93, 71, 0.06);
  color: var(--muted);
  line-height: 1.7;
}

.bonus-tip.is-hit,
.coupon-hit {
  background: rgba(138, 90, 43, 0.1);
  color: #7a4f22;
}

.coupon-picks {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 10px;
  margin: 0 0 18px;
}

.coupon-pick {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 12px 14px;
  border: 1px solid rgba(47, 93, 71, 0.12);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.84);
  text-align: left;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.coupon-pick.is-active,
.coupon-pick:hover {
  border-color: rgba(138, 90, 43, 0.4);
  box-shadow: 0 12px 24px rgba(122, 79, 34, 0.1);
  transform: translateY(-1px);
}

.coupon-pick strong {
  color: #2f2418;
}

.coupon-pick span {
  color: var(--muted);
  font-size: 13px;
}

.actions {
  display: flex;
  gap: 12px;
}

.return-tip {
  margin-top: 14px;
  color: var(--muted);
  line-height: 1.7;
}

.level-card .section-card__body {
  display: grid;
  gap: 12px;
}

.level-row {
  display: grid;
  gap: 8px;
  padding: 16px 18px;
  border-radius: 18px;
  border: 1px solid rgba(123, 88, 50, 0.12);
  background: rgba(255, 255, 255, 0.84);
}

.level-row.is-current {
  border-color: rgba(47, 93, 71, 0.3);
  box-shadow: 0 16px 32px rgba(47, 93, 71, 0.08);
}

.level-row strong,
.level-row span {
  display: block;
}

.level-row > div:first-child span {
  margin-top: 4px;
  color: var(--muted);
  font-size: 12px;
}

.level-row__meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
  color: var(--muted);
  font-size: 13px;
}

.coupon-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.coupon-header p {
  margin: 8px 0 0;
  color: var(--muted);
}

.coupon-alert {
  margin-top: 12px;
}

.coupon-list {
  display: grid;
  gap: 14px;
  margin-top: 18px;
}

.coupon-item {
  padding: 16px 18px;
  border-radius: 18px;
  border: 1px solid rgba(47, 93, 71, 0.1);
  background: linear-gradient(145deg, rgba(255, 255, 255, 0.98), rgba(249, 244, 236, 0.95));
}

.coupon-item.is-expired {
  opacity: 0.66;
}

.coupon-item__head,
.coupon-item__meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.coupon-item__head h4 {
  margin: 0 0 8px;
  font-size: 18px;
  color: #2f2418;
}

.coupon-item__head p,
.coupon-item__meta {
  margin: 0;
  color: var(--muted);
}

@media (max-width: 1080px) {
  .hero-card__body,
  .recharge-main-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .coupon-header,
  .coupon-item__head,
  .coupon-item__meta,
  .actions {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
