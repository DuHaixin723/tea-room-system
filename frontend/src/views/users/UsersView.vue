<script setup lang="ts">
// 页面文件：负责组织当前页面的数据加载、交互行为和展示内容。

import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { usersApi } from '@/api/modules/management'
import { usePagedTable } from '@/composables/usePagedTable'
import type { MemberAccountRecord, MemberRechargeRecord, UserRecord } from '@/types/business'
import PageIntro from '@/components/shared/PageIntro.vue'
import { resolveAssetUrl } from '@/utils/asset'

interface UserForm {
  nickname: string
  phone: string
  email: string
  enabled: boolean
}

const roleLabelMap: Record<UserRecord['role'], string> = {
  USER: '普通用户',
  STAFF: '茶室员',
  ADMIN: '管理员',
}

const approvalLabelMap: Record<'PENDING' | 'APPROVED' | 'REJECTED', string> = {
  PENDING: '待审核',
  APPROVED: '已通过',
  REJECTED: '已驳回',
}

const keyword = ref('')
const dialogVisible = ref(false)
const accountVisible = ref(false)
const currentRow = ref<UserRecord | null>(null)
const account = ref<MemberAccountRecord | null>(null)
const rechargeRecords = ref<MemberRechargeRecord[]>([])
const formRef = ref<FormInstance>()
const form = reactive<UserForm>({
  nickname: '',
  phone: '',
  email: '',
  enabled: true,
})

const table = usePagedTable<UserRecord>((params) => usersApi.page(params))
const currentCertificationImages = computed(() => (currentRow.value?.staffCertificationImages ?? []).map((item) => resolveAssetUrl(item)).filter(Boolean) as string[])

function certificationImagesOf(row?: UserRecord | null) {
  return (row?.staffCertificationImages ?? []).map((item) => resolveAssetUrl(item)).filter(Boolean) as string[]
}

function certificationThumbOf(row: UserRecord) {
  return certificationImagesOf(row)[0] ?? ''
}

const visibleRows = computed(() => {
  const query = keyword.value.trim().toLowerCase()
  if (!query) return table.rows
  return table.rows.filter((row) =>
    [
      row.username,
      row.nickname,
      row.phone,
      row.email ?? '',
      row.role,
      roleLabelMap[row.role],
      row.staffRealName ?? '',
      row.staffApprovalStatus ? approvalLabelMap[row.staffApprovalStatus] : '',
    ].some((item) => String(item).toLowerCase().includes(query)),
  )
})

const rules: FormRules<UserForm> = {
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { min: 2, max: 20, message: '昵称长度需在 2 到 20 个字符之间', trigger: 'blur' },
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '手机号格式不正确', trigger: 'blur' },
  ],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
}

const accountButtonLabelMap: Record<UserRecord['role'], string> = {
  USER: '会员账户',
  STAFF: '查看资料',
  ADMIN: '查看资料',
}

function resetFilters() {
  keyword.value = ''
  table.filters.role = undefined
  table.load({ page: 0 })
}

function resetForm() {
  Object.assign(form, {
    nickname: '',
    phone: '',
    email: '',
    enabled: true,
  })
  currentRow.value = null
  formRef.value?.clearValidate()
}

function roleTagType(role: UserRecord['role']) {
  if (role === 'ADMIN') return 'danger'
  if (role === 'STAFF') return 'success'
  return 'info'
}

function roleLabel(role: UserRecord['role']) {
  return roleLabelMap[role]
}

function approvalLabel(status?: UserRecord['staffApprovalStatus']) {
  return status ? approvalLabelMap[status] : '-'
}

function approvalTagType(status?: UserRecord['staffApprovalStatus']) {
  if (status === 'APPROVED') return 'success'
  if (status === 'REJECTED') return 'danger'
  if (status === 'PENDING') return 'warning'
  return 'info'
}

function accountButtonLabel(role: UserRecord['role']) {
  return accountButtonLabelMap[role]
}

function isPendingStaff(row: UserRecord) {
  return row.role === 'STAFF' && row.staffApprovalStatus === 'PENDING'
}

function openEdit(row: UserRecord) {
  currentRow.value = row
  Object.assign(form, {
    nickname: row.nickname,
    phone: row.phone,
    email: row.email ?? '',
    enabled: row.enabled,
  })
  dialogVisible.value = true
}

async function openAccount(row: UserRecord) {
  currentRow.value = row
  account.value = null
  rechargeRecords.value = []

  if (row.role !== 'USER') {
    accountVisible.value = true
    return
  }

  const [accountData, recordData] = await Promise.all([
    usersApi.account(row.id),
    usersApi.rechargeRecords(row.id),
  ])
  account.value = accountData
  rechargeRecords.value = recordData
  accountVisible.value = true
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid || !currentRow.value) return
  await usersApi.update(currentRow.value.id, form)
  ElMessage.success('用户信息已更新')
  dialogVisible.value = false
  await table.load()
}

async function approveStaff(row: UserRecord) {
  await ElMessageBox.confirm(`确认通过茶室员 ${row.nickname} 的审核吗？`, '审核茶室员', { type: 'warning' })
  await usersApi.staffApproval(row.id, { status: 'APPROVED', remark: '审核通过' })
  ElMessage.success('已通过审核')
  await table.load()
}

async function rejectStaff(row: UserRecord) {
  const { value } = await ElMessageBox.prompt(`请输入驳回 ${row.nickname} 的原因`, '驳回茶室员申请', {
    inputType: 'textarea',
    inputPlaceholder: '请输入驳回原因',
    inputValidator: (input) => (input?.trim() ? true : '请输入驳回原因'),
  })
  await usersApi.staffApproval(row.id, { status: 'REJECTED', remark: value })
  ElMessage.success('已驳回申请')
  await table.load()
}

async function removeUser(row: UserRecord) {
  await ElMessageBox.confirm(`确认删除用户“${row.nickname}”吗？`, '删除用户', { type: 'warning' })
  await usersApi.remove(row.id)
  ElMessage.success('用户已删除')
  await table.load()
}

onMounted(() => table.load())
</script>

<template>
  <div class="page-shell">
    <PageIntro title="用户管理" description="支持按角色筛选、关键字搜索、编辑资料，并审核茶室员申请资料。" />
    <div class="section-card">
      <div class="section-card__body">
        <div class="toolbar">
          <div class="toolbar-left">
            <el-input v-model="keyword" clearable placeholder="搜索账号、昵称、手机号、邮箱或审核信息" style="width: 320px" />
            <el-select v-model="table.filters.role" clearable placeholder="角色筛选" style="width: 160px" @change="table.load({ page: 0 })">
              <el-option label="普通用户" value="USER" />
              <el-option label="茶室员" value="STAFF" />
              <el-option label="管理员" value="ADMIN" />
            </el-select>
          </div>
          <div class="toolbar-right">
            <el-button @click="resetFilters">重置</el-button>
          </div>
        </div>

        <el-table v-loading="table.loading" :data="visibleRows" class="data-table">
          <el-table-column prop="username" label="账号" min-width="130" />
          <el-table-column prop="nickname" label="昵称" min-width="120" />
          <el-table-column prop="phone" label="手机号" min-width="140" />
          <el-table-column prop="email" label="邮箱" min-width="180" />
          <el-table-column label="角色" width="110">
            <template #default="{ row }">
              <el-tag :type="roleTagType(row.role)" effect="plain">{{ roleLabel(row.role) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="审核状态" width="120">
            <template #default="{ row }">
              <el-tag :type="approvalTagType(row.staffApprovalStatus)" effect="plain">{{ approvalLabel(row.staffApprovalStatus) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.enabled ? 'success' : 'danger'">{{ row.enabled ? '启用' : '停用' }}</el-tag>
            </template>
          </el-table-column>
                    <el-table-column label="资料图" width="92">
            <template #default="{ row }">
              <el-image
                v-if="row.role === 'STAFF' && certificationThumbOf(row)"
                :src="certificationThumbOf(row)"
                :preview-src-list="certificationImagesOf(row)"
                fit="cover"
                preview-teleported
                class="cert-thumb"
              />
              <span v-else>-</span>
            </template>
          </el-table-column>
<el-table-column fixed="right" label="操作" width="320">
            <template #default="{ row }">
              <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
              <el-button link type="success" @click="openAccount(row)">{{ accountButtonLabel(row.role) }}</el-button>
              <el-button v-if="isPendingStaff(row)" link type="success" @click="approveStaff(row)">通过</el-button>
              <el-button v-if="isPendingStaff(row)" link type="warning" @click="rejectStaff(row)">驳回</el-button>
              <el-button link type="danger" @click="removeUser(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-pagination
          background
          layout="total, sizes, prev, pager, next"
          :current-page="table.pager.page + 1"
          :page-size="table.pager.size"
          :total="table.pager.total"
          @current-change="table.handlePageChange"
          @size-change="table.handleSizeChange"
        />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" title="编辑用户" width="560px" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="form-grid">
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" maxlength="20" show-word-limit />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" maxlength="11" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">保存</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="accountVisible" :title="`${currentRow ? accountButtonLabel(currentRow.role) : '账户'}详情`" size="42%">
      <template v-if="currentRow">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="用户">{{ currentRow.nickname }} / {{ currentRow.username }}</el-descriptions-item>
          <el-descriptions-item label="角色">{{ roleLabel(currentRow.role) }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ currentRow.phone }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ currentRow.email || '-' }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ currentRow.enabled ? '启用' : '停用' }}</el-descriptions-item>
          <el-descriptions-item v-if="currentRow.role === 'STAFF'" label="审核状态">{{ approvalLabel(currentRow.staffApprovalStatus) }}</el-descriptions-item>
          <el-descriptions-item v-if="currentRow.role === 'STAFF'" label="真实姓名">{{ currentRow.staffRealName || '-' }}</el-descriptions-item>
          <el-descriptions-item v-if="currentRow.role === 'STAFF'" label="身份证号">{{ currentRow.staffIdCardNo || '-' }}</el-descriptions-item>
          <el-descriptions-item v-if="currentRow.role === 'STAFF'" label="补充说明">{{ currentRow.staffApplicationNote || '-' }}</el-descriptions-item>
          <el-descriptions-item v-if="currentRow.role === 'STAFF'" label="审核备注">{{ currentRow.staffApprovalRemark || '-' }}</el-descriptions-item>
          <el-descriptions-item v-if="currentRow.role === 'STAFF'" label="审核资料">
            <div v-if="currentCertificationImages.length" class="cert-preview-grid">
              <div v-for="(image, index) in currentCertificationImages" :key="`${image}-${index}`" class="cert-preview">
                <el-image :src="image" :preview-src-list="currentCertificationImages" fit="cover" preview-teleported class="cert-preview__image" />
                <a :href="image" target="_blank" rel="noreferrer">新开查看</a>
              </div>
            </div>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item v-if="currentRow.role === 'USER'" label="当前余额">￥{{ account?.balance ?? 0 }}</el-descriptions-item>
          <el-descriptions-item v-if="currentRow.role === 'USER'" label="会员等级">{{ account?.memberLevel ?? 'NORMAL' }}</el-descriptions-item>
        </el-descriptions>

        <div v-if="currentRow.role === 'USER'" class="record-title">充值流水</div>
        <el-table v-if="currentRow.role === 'USER'" :data="rechargeRecords" size="small" empty-text="暂无充值记录">
          <el-table-column prop="createdAt" label="时间" min-width="160" />
          <el-table-column prop="amount" label="充值金额" width="120">
            <template #default="{ row }">￥{{ row.amount }}</template>
          </el-table-column>
          <el-table-column prop="balanceAfter" label="充值后余额" width="130">
            <template #default="{ row }">￥{{ row.balanceAfter }}</template>
          </el-table-column>
          <el-table-column prop="remark" label="备注" min-width="120" />
        </el-table>
      </template>
    </el-drawer>
  </div>
</template>

<style scoped>
.record-title {
  margin: 18px 0 10px;
  font-weight: 700;
}

.cert-thumb {
  width: 44px;
  height: 44px;
  overflow: hidden;
  border-radius: 10px;
  border: 1px solid rgba(88, 72, 53, 0.12);
}

.cert-preview-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 12px;
}

.cert-preview {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 10px;
}

.cert-preview__image {
  width: 180px;
  height: 120px;
  overflow: hidden;
  border: 1px solid rgba(88, 72, 53, 0.12);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.9);
}
</style>
