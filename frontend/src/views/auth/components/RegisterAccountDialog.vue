<script setup lang="ts">
// 共享组件：封装可复用的界面块与交互单元。

import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules, UploadFile, UploadRawFile } from 'element-plus'
import { UserFilled, Briefcase, UploadFilled, CircleCheckFilled } from '@element-plus/icons-vue'
import { register } from '@/api/modules/auth'
import { fileApi } from '@/api/modules/management'
import type { RegisterPayload } from '@/types/auth'

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const formRef = ref<FormInstance>()
const submitting = ref(false)
const uploading = ref(false)

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const form = reactive<RegisterPayload>({
  username: '',
  password: '',
  nickname: '',
  phone: '',
  email: '',
  role: 'USER',
  staffRealName: '',
  staffIdCardNo: '',
  staffCertificationImages: undefined,
  staffApplicationNote: '',
})

const isStaff = computed(() => form.role === 'STAFF')
const certificationUploaded = computed(() => (form.staffCertificationImages?.length ?? 0) > 0)

const rules: FormRules<typeof form> = {
  username: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { min: 4, max: 20, message: '账号长度需在 4 到 20 个字符之间', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度需在 6 到 20 个字符之间', trigger: 'blur' },
  ],
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { min: 2, max: 20, message: '昵称长度需在 2 到 20 个字符之间', trigger: 'blur' },
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '手机号格式不正确', trigger: 'blur' },
  ],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
  staffRealName: [
    {
      validator: (_rule, value, callback) => {
        if (isStaff.value && !value) callback(new Error('请输入真实姓名'))
        else callback()
      },
      trigger: 'blur',
    },
  ],
  staffIdCardNo: [
    {
      validator: (_rule, value, callback) => {
        if (isStaff.value && !value) callback(new Error('请输入身份证号'))
        else callback()
      },
      trigger: 'blur',
    },
  ],
  staffCertificationImages: [
    {
      validator: (_rule, value, callback) => {
        if (isStaff.value && !value) callback(new Error('请上传审核资料'))
        else callback()
      },
      trigger: 'change',
    },
  ],
}

function resetForm() {
  Object.assign(form, {
    username: '',
    password: '',
    nickname: '',
    phone: '',
    email: '',
    role: 'USER',
    staffRealName: '',
    staffIdCardNo: '',
    staffCertificationImages: undefined,
    staffApplicationNote: '',
  })
  formRef.value?.clearValidate()
}

watch(
  () => visible.value,
  (next) => {
    if (!next) resetForm()
  },
)

watch(
  () => form.role,
  (role) => {
    if (role === 'USER') {
      form.staffRealName = ''
      form.staffIdCardNo = ''
      form.staffCertificationImages = undefined
      form.staffApplicationNote = ''
    }
    formRef.value?.clearValidate(['staffRealName', 'staffIdCardNo', 'staffCertificationImages'])
  },
)

async function uploadCredential(file: UploadRawFile) {
  if (!file.type?.startsWith('image/')) {
    ElMessage.error('只能上传图片文件')
    return false
  }
  if (file.size / 1024 / 1024 > 8) {
    ElMessage.error('审核资料图片不能超过 8MB')
    return false
  }

  uploading.value = true
  try {
    const imageUrl = await fileApi.uploadPublicImage(file)
    form.staffCertificationImages = [...(form.staffCertificationImages ?? []), imageUrl]
    ElMessage.success('审核资料上传成功')
    formRef.value?.validateField('staffCertificationImages').catch(() => undefined)
  } finally {
    uploading.value = false
  }
  return false
}

function onUploadChange(file: UploadFile) {
  if (!file.raw) return
  void uploadCredential(file.raw)
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const payload: RegisterPayload = {
      username: form.username,
      password: form.password,
      nickname: form.nickname,
      phone: form.phone,
      email: form.email || undefined,
      role: form.role,
    }

    if (form.role === 'STAFF') {
      payload.staffRealName = form.staffRealName
      payload.staffIdCardNo = form.staffIdCardNo
      payload.staffCertificationImages = form.staffCertificationImages
      payload.staffApplicationNote = form.staffApplicationNote || undefined
    }

    await register(payload)
    ElMessage.success(form.role === 'STAFF' ? '申请已提交，请等待管理员审核' : '注册成功，请直接登录')
    visible.value = false
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <el-dialog v-model="visible" class="register-dialog" width="720px" top="6vh" :destroy-on-close="true">
    <div class="register-card">
      <header class="register-card__hero">
        <div class="register-card__hero-main">
          <span class="register-card__eyebrow">Create Account</span>
          <h2 class="register-card__title">注册账号</h2>
          <p class="register-card__desc">
            普通用户提交后可直接登录。茶室员需要补充实名和资质材料，审核通过后再启用账号。
          </p>
        </div>
        <div class="register-card__status" :class="{ 'is-staff': isStaff }">
          <span class="register-card__status-label">当前模式</span>
          <strong>{{ isStaff ? '茶室员申请' : '普通用户注册' }}</strong>
        </div>
      </header>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="register-form">
        <section class="register-panel">
          <div class="register-panel__head">
            <div>
              <h3>注册身份</h3>
              <p>先选择账号用途，再填写对应资料。</p>
            </div>
          </div>

          <div class="role-switch">
            <button type="button" class="role-switch__item" :class="{ 'is-active': form.role === 'USER' }" @click="form.role = 'USER'">
              <span class="role-switch__icon role-switch__icon--user">
                <el-icon><UserFilled /></el-icon>
              </span>
              <span class="role-switch__body">
                <strong>普通用户</strong>
                <small>立即可用，用于预约茶室、购茶和查看订单。</small>
              </span>
            </button>

            <button type="button" class="role-switch__item" :class="{ 'is-active': form.role === 'STAFF' }" @click="form.role = 'STAFF'">
              <span class="role-switch__icon role-switch__icon--staff">
                <el-icon><Briefcase /></el-icon>
              </span>
              <span class="role-switch__body">
                <strong>茶室员</strong>
                <small>需要审核后启用，用于接待与服务工作。</small>
              </span>
            </button>
          </div>
        </section>

        <section class="register-panel">
          <div class="register-panel__head">
            <div>
              <h3>基础信息</h3>
              <p>用于创建登录账号和联系信息。</p>
            </div>
          </div>

          <div class="register-grid">
            <el-form-item label="账号" prop="username">
              <el-input v-model="form.username" maxlength="20" placeholder="4-20 位字母、数字或组合" />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input v-model="form.password" type="password" show-password maxlength="20" placeholder="6-20 位登录密码" />
            </el-form-item>
            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="form.nickname" maxlength="20" placeholder="页面中展示的名称" />
            </el-form-item>
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="form.phone" maxlength="11" placeholder="用于联系和账号校验" />
            </el-form-item>
            <el-form-item class="register-grid__full" label="邮箱" prop="email">
              <el-input v-model="form.email" placeholder="选填，用于接收通知" />
            </el-form-item>
          </div>
        </section>

        <section v-if="isStaff" class="register-panel register-panel--staff">
          <div class="register-panel__head">
            <div>
              <h3>审核资料</h3>
              <p>请填写实名信息并上传可识别的资质或证明材料。</p>
            </div>
            <div class="register-tip">单张图片不超过 8MB</div>
          </div>

          <div class="register-grid">
            <el-form-item label="真实姓名" prop="staffRealName">
              <el-input v-model="form.staffRealName" maxlength="50" placeholder="与证件一致" />
            </el-form-item>
            <el-form-item label="身份证号" prop="staffIdCardNo">
              <el-input v-model="form.staffIdCardNo" maxlength="30" placeholder="用于管理员核验" />
            </el-form-item>
          </div>

          <el-form-item label="审核资料图片" prop="staffCertificationImages">
            <div class="upload-box" :class="{ 'is-ready': certificationUploaded }">
              <div class="upload-box__info">
                <span class="upload-box__icon">
                  <el-icon v-if="certificationUploaded"><CircleCheckFilled /></el-icon>
                  <el-icon v-else><UploadFilled /></el-icon>
                </span>
                <div>
                  <strong>{{ certificationUploaded ? '资料已上传' : '上传资质图片' }}</strong>
                  <p>{{ certificationUploaded ? '可继续查看或重新上传资料。' : '建议上传身份证明、证书或培训证明。' }}</p>
                </div>
              </div>
              <div class="upload-box__ops">
                <el-upload :show-file-list="false" :auto-upload="false" accept="image/*" :on-change="onUploadChange">
                  <el-button type="primary" plain :loading="uploading">{{ certificationUploaded ? '重新上传' : '上传资料' }}</el-button>
                </el-upload>
                <span v-if="form.staffCertificationImages?.length">已上传 {{ form.staffCertificationImages.length }} 张</span>
              </div>
            </div>
          </el-form-item>

          <el-form-item label="补充说明">
            <el-input
              v-model="form.staffApplicationNote"
              type="textarea"
              :rows="4"
              maxlength="500"
              show-word-limit
              placeholder="可补充从业经历、擅长方向或希望管理员了解的信息"
            />
          </el-form-item>
        </section>
      </el-form>

      <footer class="register-card__footer">
        <p class="register-card__footer-tip">
          {{ isStaff ? '提交后进入审核队列，审核通过后才能登录。' : '提交成功后即可使用账号登录。' }}
        </p>
        <div class="register-card__footer-actions">
          <el-button @click="visible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="submit">提交注册</el-button>
        </div>
      </footer>
    </div>
  </el-dialog>
</template>

<style scoped>
:deep(.register-dialog .el-dialog) {
  overflow: hidden;
  border-radius: 26px;
  background: #f9f6f0;
  box-shadow: 0 30px 80px rgba(41, 35, 27, 0.16);
}

:deep(.register-dialog .el-dialog__header) {
  display: none;
}

:deep(.register-dialog .el-dialog__body) {
  padding: 0;
}

:deep(.register-dialog .el-dialog__footer) {
  display: none;
}

.register-card {
  background:
    radial-gradient(circle at top right, rgba(58, 121, 200, 0.08), transparent 26%),
    radial-gradient(circle at top left, rgba(211, 165, 89, 0.12), transparent 28%),
    linear-gradient(180deg, #fcfbf8, #f6f1e8 100%);
}

.register-card__hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  padding: 26px 28px 20px;
  border-bottom: 1px solid rgba(88, 72, 53, 0.08);
}

.register-card__eyebrow {
  display: inline-block;
  color: #4e7ebf;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.register-card__title {
  margin: 10px 0 0;
  font-size: 34px;
  line-height: 1.05;
  letter-spacing: -0.05em;
}

.register-card__desc {
  max-width: 500px;
  margin: 12px 0 0;
  color: var(--muted);
  line-height: 1.75;
}

.register-card__status {
  min-width: 156px;
  padding: 14px 16px;
  border: 1px solid rgba(58, 121, 200, 0.18);
  border-radius: 18px;
  background: rgba(58, 121, 200, 0.08);
}

.register-card__status.is-staff {
  border-color: rgba(45, 107, 83, 0.22);
  background: rgba(45, 107, 83, 0.09);
}

.register-card__status-label {
  display: block;
  color: var(--muted);
  font-size: 12px;
}

.register-card__status strong {
  display: block;
  margin-top: 8px;
  font-size: 18px;
}

.register-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 18px 20px 0;
}

.register-panel {
  padding: 18px;
  border: 1px solid rgba(88, 72, 53, 0.1);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.78);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.7);
}

.register-panel--staff {
  background:
    linear-gradient(180deg, rgba(246, 251, 248, 0.94), rgba(240, 247, 243, 0.98)),
    linear-gradient(145deg, rgba(45, 107, 83, 0.05), transparent);
}

.register-panel__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}

.register-panel__head h3 {
  margin: 0;
  font-size: 18px;
}

.register-panel__head p {
  margin: 6px 0 0;
  color: var(--muted);
  line-height: 1.7;
}

.register-tip {
  padding: 7px 10px;
  border-radius: 999px;
  background: rgba(45, 107, 83, 0.1);
  color: #255844;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}

.role-switch {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.role-switch__item {
  appearance: none;
  border: 1px solid rgba(88, 72, 53, 0.12);
  border-radius: 18px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(247, 244, 238, 0.96));
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px;
  cursor: pointer;
  text-align: left;
  transition:
    transform 140ms ease,
    border-color 140ms ease,
    box-shadow 140ms ease,
    background 140ms ease;
}

.role-switch__item:hover {
  transform: translateY(-1px);
  border-color: rgba(88, 72, 53, 0.24);
  box-shadow: 0 12px 26px rgba(52, 41, 26, 0.08);
}

.role-switch__item.is-active {
  border-color: rgba(58, 121, 200, 0.34);
  background: linear-gradient(180deg, rgba(232, 241, 252, 0.98), rgba(246, 249, 255, 0.98));
  box-shadow: 0 14px 30px rgba(58, 121, 200, 0.1);
}

.role-switch__icon {
  display: grid;
  place-items: center;
  width: 42px;
  height: 42px;
  border-radius: 14px;
  flex: 0 0 42px;
  font-size: 18px;
}

.role-switch__icon--user {
  color: #2f62a3;
  background: rgba(58, 121, 200, 0.12);
}

.role-switch__icon--staff {
  color: #2d6b53;
  background: rgba(45, 107, 83, 0.12);
}

.role-switch__body {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.role-switch__body strong {
  font-size: 15px;
}

.role-switch__body small {
  color: var(--muted);
  line-height: 1.6;
}

.register-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

.register-grid__full {
  grid-column: 1 / -1;
}

.upload-box {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px;
  border: 1px dashed rgba(88, 72, 53, 0.2);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.76);
}

.upload-box.is-ready {
  border-color: rgba(45, 107, 83, 0.3);
  background: rgba(244, 250, 247, 0.98);
}

.upload-box__info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.upload-box__icon {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  border-radius: 14px;
  background: rgba(45, 107, 83, 0.12);
  color: #2d6b53;
  font-size: 18px;
}

.upload-box__info strong {
  display: block;
  font-size: 15px;
}

.upload-box__info p {
  margin: 5px 0 0;
  color: var(--muted);
  line-height: 1.6;
}

.upload-box__ops {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.upload-box__ops a {
  color: #2f62a3;
  font-weight: 600;
}

.register-card__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 28px 24px;
  margin-top: 10px;
  border-top: 1px solid rgba(88, 72, 53, 0.08);
}

.register-card__footer-tip {
  margin: 0;
  color: var(--muted);
  line-height: 1.7;
}

.register-card__footer-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

@media (max-width: 768px) {
  :deep(.register-dialog .el-dialog) {
    width: calc(100vw - 18px) !important;
    margin: 10px auto;
  }

  .register-card__hero,
  .register-panel__head,
  .upload-box,
  .register-card__footer {
    flex-direction: column;
    align-items: flex-start;
  }

  .register-form {
    padding: 16px 16px 0;
  }

  .register-card__hero,
  .register-card__footer {
    padding-left: 16px;
    padding-right: 16px;
  }

  .role-switch,
  .register-grid {
    grid-template-columns: 1fr;
  }

  .upload-box__ops,
  .register-card__footer-actions {
    width: 100%;
    justify-content: flex-end;
  }

  .register-card__status {
    width: 100%;
  }
}
</style>
