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

      <el-divider content-position="left">法定节假日（自动同步）</el-divider>
      <el-form-item label="自动同步">
        <div>
          <el-button size="small" :loading="syncing" @click="onSync">立即同步</el-button>
          <span class="hint">系统每天凌晨自动从公开节假日库同步一次，含调休补班安排</span>
        </div>
        <div style="margin-top: 10px" v-loading="holidayLoading">
          <el-tag v-for="h in holidays" :key="h.holidayDate" class="holiday-tag"
            :type="h.offDay ? 'danger' : 'success'" effect="plain">
            {{ h.holidayDate }} {{ h.name }}（{{ h.offDay ? '停餐' : '照常开餐' }}）
          </el-tag>
          <el-empty v-if="!holidayLoading && !holidays.length" description="暂无数据，点击上方“立即同步”获取" :image-size="60" />
        </div>
      </el-form-item>

      <el-divider content-position="left">手动调整（优先级高于自动节假日）</el-divider>
      <el-form-item label="停餐日期">
        <el-input v-model="form.closedDates" type="textarea" :rows="4"
          placeholder="额外的不开餐日期，每行一个，格式 2026-10-01" />
        <span class="hint">这些日期不开餐：员工报餐 / 预订 / 一键报本周本月 都会自动跳过</span>
      </el-form-item>
      <el-form-item label="补班开餐日">
        <el-input v-model="form.openDates" type="textarea" :rows="3"
          placeholder="需要强制开餐的日期（覆盖自动节假日/周末规则），每行一个，格式 2026-10-11" />
        <span class="hint">这些日期强制照常开餐，优先级最高，可用于纠正自动同步的节假日安排</span>
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
import { getConfig, saveConfig, getHolidays, syncHolidays } from '../api/bc'

const loading = ref(false)
const saving = ref(false)
const holidays = ref([])
const holidayLoading = ref(false)
const syncing = ref(false)
const form = reactive({
  id: null,
  userNeedApprove: true,
  lunchCanMeal: true,
  dinnerCanMeal: true,
  lunchOrderTime: '09:30',
  dinnerOrderTime: '15:30',
  saturdayCanDiner: false,
  sundayCanDiner: false,
  closedDates: '',
  openDates: ''
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
        sundayCanDiner: !!cfg.sundayCanDiner,
        closedDates: cfg.closedDates || '',
        openDates: cfg.openDates || ''
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
      id: form.id || undefined,
      userNeedApprove: form.userNeedApprove,
      lunchCanMeal: form.lunchCanMeal,
      dinnerCanMeal: form.dinnerCanMeal,
      lunchOrderTime: form.lunchOrderTime || '',
      dinnerOrderTime: form.dinnerOrderTime || '',
      saturdayCanDiner: form.saturdayCanDiner,
      sundayCanDiner: form.sundayCanDiner,
      closedDates: form.closedDates || '',
      openDates: form.openDates || ''
    })
    ElMessage.success('保存成功')
    load()
  } catch (e) {
    /* 已提示 */
  } finally {
    saving.value = false
  }
}

async function loadHolidays() {
  holidayLoading.value = true
  try {
    const year = new Date().getFullYear()
    holidays.value = (await getHolidays(year)) || []
  } finally {
    holidayLoading.value = false
  }
}

async function onSync() {
  syncing.value = true
  try {
    await syncHolidays()
    ElMessage.success('同步成功')
    loadHolidays()
  } catch (e) {
    /* 已提示 */
  } finally {
    syncing.value = false
  }
}

onMounted(() => {
  load()
  loadHolidays()
})
</script>

<style scoped>
.hint {
  margin-left: 12px;
  color: #909399;
  font-size: 12px;
}
.holiday-tag {
  margin: 0 8px 8px 0;
}
</style>
