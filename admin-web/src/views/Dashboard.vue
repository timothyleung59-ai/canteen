<template>
  <div>
    <el-row :gutter="16">
      <el-col :xs="12" :sm="6" v-for="c in cards" :key="c.label">
        <el-card shadow="hover" class="stat">
          <div class="stat-icon" :style="{ background: c.color }">
            <el-icon :size="26"><component :is="c.icon" /></el-icon>
          </div>
          <div class="stat-body">
            <div class="stat-num">{{ c.value }}</div>
            <div class="stat-label">{{ c.label }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="page-card" style="margin-top: 16px" v-loading="loading">
      <template #header>各部门人数（已注册员工）</template>
      <el-table :data="deptRows" stripe>
        <el-table-column type="index" label="#" width="60" />
        <el-table-column prop="name" label="部门" />
        <el-table-column prop="headcount" label="员工人数" />
      </el-table>
      <el-empty v-if="!deptRows.length" description="暂无部门数据" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onActivated, onMounted } from 'vue'
import {
  getTotalRecordByDinTime,
  getUserPageList,
  getDepartmentList,
  getDepartmentPageList
} from '../api/bc'

const loading = ref(false)
const today = ref(0)
const tomorrow = ref(0)
const empTotal = ref(0)
const deptTotal = ref(0)
const deptRows = ref([])

const cards = ref([])
function buildCards() {
  cards.value = [
    { label: '今日报餐', value: today.value, icon: 'Bowl', color: '#409eff' },
    { label: '明日报餐', value: tomorrow.value, icon: 'Calendar', color: '#67c23a' },
    { label: '员工总数', value: empTotal.value, icon: 'User', color: '#e6a23c' },
    { label: '部门数', value: deptTotal.value, icon: 'OfficeBuilding', color: '#909399' }
  ]
}

async function load() {
  loading.value = true
  try {
    const [t0, t1, emp, depts, deptPage] = await Promise.all([
      getTotalRecordByDinTime(0),
      getTotalRecordByDinTime(1),
      getUserPageList({ currentPage: 1, pageSize: 1 }),
      getDepartmentList(),
      getDepartmentPageList(1, 100)
    ])
    today.value = t0 || 0
    tomorrow.value = t1 || 0
    empTotal.value = (emp && emp.total) || 0
    deptTotal.value = (depts && depts.length) || 0
    const deptArr = (deptPage && deptPage.data) || []
    deptRows.value = (deptArr.length ? deptArr : depts || []).map((d) => ({
      name: d.name,
      headcount: d.headcount != null ? d.headcount : '-'
    }))
  } finally {
    loading.value = false
  }
  buildCards()
}

onMounted(load)
onActivated(load)
buildCards()
</script>

<style scoped>
.stat {
  display: flex;
  align-items: center;
}
.stat :deep(.el-card__body) {
  display: flex;
  align-items: center;
  gap: 14px;
  width: 100%;
}
.stat-icon {
  width: 52px;
  height: 52px;
  border-radius: 10px;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
}
.stat-num {
  font-size: 26px;
  font-weight: 700;
  line-height: 1;
}
.stat-label {
  color: #909399;
  font-size: 13px;
  margin-top: 6px;
}
</style>
