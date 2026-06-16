<template>
  <div class="page-card" style="max-width: 640px" v-loading="loading">
    <h3 style="margin-top: 0">报餐设置</h3>
    <el-form label-width="160px">
      <el-form-item label="新员工需要审核">
        <el-switch v-model="form.userNeedApprove" />
        <span class="hint">开启后，新注册员工需管理员激活才能报餐</span>
      </el-form-item>

      <el-divider content-position="left">午餐</el-divider>
      <el-form-item label="开放午餐报餐">
        <el-switch v-model="form.lunchCanMeal" />
      </el-form-item>
      <el-form-item label="午餐报餐截止时间">
        <el-time-picker v-model="form.lunchOrderTime" format="HH:mm" value-format="HH:mm"
          placeholder="如 09:30" :disabled="!form.lunchCanMeal" />
      </el-form-item>

      <el-divider content-position="left">晚餐</el-divider>
      <el-form-item label="开放晚餐报餐">
        <el-switch v-model="form.dinnerCanMeal" />
      </el-form-item>
      <el-form-item label="晚餐报餐截止时间">
        <el-time-picker v-model="form.dinnerOrderTime" format="HH:mm" value-format="HH:mm"
          placeholder="如 15:30" :disabled="!form.dinnerCanMeal" />
      </el-form-item>

      <el-divider content-position="left">周末</el-divider>
      <el-form-item label="周六可报餐">
        <el-switch v-model="form.saturdayCanDiner" />
      </el-form-item>
      <el-form-item label="周日可报餐">
        <el-switch v-model="form.sundayCanDiner" />
      </el-form-item>

      <el-form-item>
        <el-button type="primary" :loading="saving" @click="save">保存设置</el-button>
        <el-button @click="load">重置</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getConfig, saveConfig } from '../api/bc'

const loading = ref(false)
const saving = ref(false)
const form = reactive({
  id: null,
  userNeedApprove: true,
  lunchCanMeal: true,
  dinnerCanMeal: true,
  lunchOrderTime: '09:30',
  dinnerOrderTime: '15:30',
  saturdayCanDiner: false,
  sundayCanDiner: false
})

async function load() {
  loading.value = true
  try {
    const cfg = await getConfig()
    if (cfg) {
      Object.assign(form, {
        id: cfg.id ?? null,
        userNeedApprove: !!cfg.userNeedApprove,
        lunchCanMeal: !!cfg.lunchCanMeal,
        dinnerCanMeal: !!cfg.dinnerCanMeal,
        lunchOrderTime: cfg.lunchOrderTime || '09:30',
        dinnerOrderTime: cfg.dinnerOrderTime || '15:30',
        saturdayCanDiner: !!cfg.saturdayCanDiner,
        sundayCanDiner: !!cfg.sundayCanDiner
      })
    }
  } finally {
    loading.value = false
  }
}

async function save() {
  saving.value = true
  try {
    await saveConfig({
      userNeedApprove: form.userNeedApprove,
      lunchCanMeal: form.lunchCanMeal,
      dinnerCanMeal: form.dinnerCanMeal,
      lunchOrderTime: form.lunchOrderTime || '',
      dinnerOrderTime: form.dinnerOrderTime || '',
      saturdayCanDiner: form.saturdayCanDiner,
      sundayCanDiner: form.sundayCanDiner
    })
    ElMessage.success('保存成功')
    load()
  } catch (e) {
    /* 已提示 */
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.hint {
  margin-left: 12px;
  color: #909399;
  font-size: 12px;
}
</style>
