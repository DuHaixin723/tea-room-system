<script setup lang="ts">
// 页面文件：负责组织当前页面的数据加载、交互行为和展示内容。

import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules, UploadFile, UploadRawFile } from 'element-plus'
import { UserFilled, Briefcase, UploadFilled, CircleCheckFilled, ArrowLeft } from '@element-plus/icons-vue'
import LoginShell from '@/views/auth/components/LoginShell.vue'
import { register } from '@/api/modules/auth'
import { fileApi } from '@/api/modules/management'
import type { RegisterPayload } from '@/types/auth'

const router = useRouter()
const formRef = ref<FormInstance>()
const submitting = ref(false)
const uploading = ref(false)

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
        if (isStaff.value && (!Array.isArray(value) || value.length === 0)) {
          callback(new Error('请上传审核资料图片'))
          return
        }
        callback()
      },
      trigger: 'change',
    },
  ],
}

function goBack() {
  router.push('/login/user')
}

function goStaffLogin() {
  router.push('/login/staff')
}

function switchRole(role: 'USER' | 'STAFF') {
  form.role = role
  if (role === 'USER') {
    form.staffRealName = ''
    form.staffIdCardNo = ''
    form.staffCertificationImages = undefined
    form.staffApplicationNote = ''
  }
  formRef.value?.clearValidate(['staffRealName', 'staffIdCardNo', 'staffCertificationImages'])
}

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

function removeCredential(index: number) {
  const nextImages = (form.staffCertificationImages ?? []).filter((_, currentIndex) => currentIndex !== index)
  form.staffCertificationImages = nextImages.length ? nextImages : undefined
  formRef.value?.validateField('staffCertificationImages').catch(() => undefined)
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
    router.push(form.role === 'STAFF' ? '/login/staff' : '/login/user')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <LoginShell
    title="创建账号"
    subtitle="填写基础信息即可注册。茶室员提交后会进入管理员审核流程。"
    heading="选择适合你的平台入口"
    description="普通用户注册后可直接登录；茶室员需要补充实名与审核资料，审核通过后才会启用账号。"
    :highlights="[
      { title: '普通用户', description: '注册后即可预约茶室、购买茶叶和查看订单。' },
      { title: '茶室员申请', description: '提交实名和多张审核资料图片，管理员审核通过后开通。' },
    ]"
    variant="client"
  >
    <div class="register-page">
      <div class="register-page__topbar">
        <button type="button" class="register-page__back" @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          返回用户登录
        </button>
        <div class="register-page__topbar-right">
          <button type="button" class="register-page__switch" @click="goStaffLogin">去茶室员登录</button>
          <div class="register-page__mode" :class="{ 'is-staff': isStaff }">
            {{ isStaff ? '当前为茶室员申请模式' : '当前为普通用户注册模式' }}
          </div>
        </div>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="register-form">
        <section class="register-panel">
          <div class="register-panel__head">
            <div>
              <h3>选择身份</h3>
              <p>可在普通用户与茶室员申请之间切换，系统会展示对应的注册要求。</p>
            </div>
          </div>

          <div class="role-switch">
            <button type="button" class="role-switch__item" :class="{ 'is-active': form.role === 'USER' }" @click="switchRole('USER')">
              <span class="role-switch__icon role-switch__icon--user">
                <el-icon><UserFilled /></el-icon>
              </span>
              <span class="role-switch__body">
                <strong>普通用户</strong>
                <small>提交后可直接登录，适合预约、商城购买和个人中心使用。</small>
              </span>
            </button>

            <button type="button" class="role-switch__item" :class="{ 'is-active': form.role === 'STAFF' }" @click="switchRole('STAFF')">
              <span class="role-switch__icon role-switch__icon--staff">
                <el-icon><Briefcase /></el-icon>
              </span>
              <span class="role-switch__body">
                <strong>茶室员</strong>
                <small>需提交审核资料并等待审核，审核通过后进入茶室员工作台。</small>
              </span>
            </button>
          </div>
        </section>

        <section class="register-panel">
          <div class="register-panel__head">
            <div>
              <h3>基础信息</h3>
              <p>用于创建登录账号与基础联系方式。</p>
            </div>
          </div>

          <div class="register-grid">
            <el-form-item label="账号" prop="username">
              <el-input v-model="form.username" maxlength="20" placeholder="4-20 位字母或数字组合" />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input v-model="form.password" type="password" show-password maxlength="20" placeholder="6-20 位登录密码" />
            </el-form-item>
            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="form.nickname" maxlength="20" placeholder="页面展示昵称" />
            </el-form-item>
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="form.phone" maxlength="11" placeholder="用于联系与账号校验" />
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
              <p>请填写实名信息，并上传可识别的资格证明、培训证明或相关证件图片。</p>
            </div>
            <div class="register-tip">支持多张图片，单张不超过 8MB</div>
          </div>

          <div class="register-grid">
            <el-form-item label="真实姓名" prop="staffRealName">
              <el-input v-model="form.staffRealName" maxlength="50" placeholder="请填写证件上的真实姓名" />
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
                  <strong>{{ certificationUploaded ? `已上传 ${form.staffCertificationImages?.length ?? 0} 张资料图` : '上传审核资料图片' }}</strong>
                  <p>{{ certificationUploaded ? '可继续追加上传，也可以删除不需要的图片。' : '建议上传身份证明、资格证书、培训证明等材料。' }}</p>
                </div>
              </div>
              <div class="upload-box__ops">
                <el-upload :show-file-list="false" :auto-upload="false" accept="image/*" :on-change="onUploadChange">
                  <el-button type="primary" plain :loading="uploading">
                    {{ certificationUploaded ? '继续上传' : '上传资料' }}
                  </el-button>
                </el-upload>
              </div>
            </div>

            <div v-if="form.staffCertificationImages?.length" class="credential-grid">
              <div v-for="(image, index) in form.staffCertificationImages" :key="`${image}-${index}`" class="credential-card">
                <a :href="image" target="_blank" rel="noreferrer">
                  <img :src="image" alt="credential" class="credential-card__image" />
                </a>
                <div class="credential-card__actions">
                  <a :href="image" target="_blank" rel="noreferrer">查看</a>
                  <button type="button" @click="removeCredential(index)">删除</button>
                </div>
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
              placeholder="可补充从业经历、擅长方向，或希望管理员了解的信息"
            />
          </el-form-item>
        </section>

        <div class="register-actions">
          <p class="register-actions__tip">
            {{ isStaff ? '提交后账号会进入审核队列，审核通过后才能登录。' : '提交成功后即可使用账号登录。' }}
          </p>
          <div class="register-actions__buttons">
            <el-button @click="goBack">取消</el-button>
            <el-button type="primary" :loading="submitting" @click="submit">提交注册</el-button>
          </div>
        </div>
      </el-form>
    </div>
  </LoginShell>
</template>

<style scoped>
.register-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.register-page__topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.register-page__back {
  appearance: none;
  border: 0;
  background: transparent;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 0;
  color: var(--muted);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}

.register-page__topbar-right {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.register-page__switch {
  appearance: none;
  border: 1px solid rgba(45, 107, 83, 0.18);
  border-radius: 999px;
  background:
    linear-gradient(180deg, rgba(239, 248, 244, 0.98), rgba(227, 241, 234, 0.94)),
    linear-gradient(135deg, rgba(45, 107, 83, 0.08), transparent);
  padding: 10px 16px;
  color: #255844;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: transform 140ms ease, box-shadow 140ms ease, border-color 140ms ease;
}

.register-page__switch:hover {
  transform: translateY(-1px);
  border-color: rgba(45, 107, 83, 0.3);
  box-shadow: 0 12px 24px rgba(45, 107, 83, 0.14);
}

.register-page__mode {
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(58, 121, 200, 0.1);
  color: #2f62a3;
  font-size: 12px;
  font-weight: 700;
}

.register-page__mode.is-staff {
  background: rgba(45, 107, 83, 0.12);
  color: #2d6b53;
}

.register-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.register-panel {
  padding: 18px;
  border: 1px solid rgba(88, 72, 53, 0.1);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.78);
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
  transition: transform 140ms ease, border-color 140ms ease, box-shadow 140ms ease, background 140ms ease;
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

.credential-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 12px;
  margin-top: 12px;
}

.credential-card {
  overflow: hidden;
  border: 1px solid rgba(88, 72, 53, 0.12);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.92);
}

.credential-card__image {
  display: block;
  width: 100%;
  height: 104px;
  object-fit: cover;
}

.credential-card__actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 10px 12px;
}

.credential-card__actions a,
.credential-card__actions button {
  color: #2f62a3;
  font-size: 13px;
  font-weight: 600;
  background: transparent;
  border: none;
  padding: 0;
  cursor: pointer;
}

.register-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.register-actions__tip {
  margin: 0;
  color: var(--muted);
  line-height: 1.7;
}

.register-actions__buttons {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

@media (max-width: 768px) {
  .register-page__topbar,
  .register-panel__head,
  .upload-box,
  .register-actions {
    flex-direction: column;
    align-items: flex-start;
  }

  .register-page__topbar-right {
    width: 100%;
    justify-content: space-between;
  }

  .role-switch,
  .register-grid {
    grid-template-columns: 1fr;
  }

  .upload-box__ops,
  .register-actions__buttons {
    width: 100%;
    justify-content: flex-end;
  }
}
</style>
