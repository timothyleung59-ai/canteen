<template>
  <div class="page-card">
    <div class="toolbar">
      <el-input v-model="q.name" placeholder="姓名" clearable style="width: 140px" @keyup.enter="search" />
      <el-input v-model="q.mobile" placeholder="手机号" clearable style="width: 150px" @keyup.enter="search" />
      <el-select v-model="q.departmentId" placeholder="部门" clearable style="width: 150px">
        <el-option v-for="d in depts" :key="d.id" :label="d.name" :value="d.id" />
      </el-select>
      <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="开始日期"
        end-placeholder="结束日期" value-format="YYYY-MM-DD" style="width: 240px" />
      <el-button type="primary" :icon="Search" @click="search">查询</el-button>
      <el-button :icon="Refresh" @click="reset">重置</el-button>
      <div class="grow"></div>
      <el-button type="success" :icon="Download" :loading="exporting" @click="onExport">导出 Excel</el-button>
    </div>

    <!-- 手机端: 卡片列表 -->
    <div v-if="isMobile" v-loading="loading">
      <div v-for="(row, idx) in rows" :key="idx" class="mobile-card">
        <div class="mobile-card-header">
          <span class="mobile-card-title">{{ row.name }}</span>
          <el-tag :type="row.bcChannel === '预约报餐' ? 'warning' : 'success'" effect="plain">
            {{ row.bcChannel === '预约报餐' ? '预约' : '已报' }}
          </el-tag>
        </div>
        <div class="mobile-card-row"><span class="k">报餐时间</span><span class="v">{{ row.dinTime }}</span></div>
        <div class="mobile-card-row"><span class="k">手机号</span><span class="v">{{ row.mobile }}</span></div>
        <div class="mobile-card-row"><span class="k">部门</span><span class="v">{{ row.deptName }}</span></div>
        <div class="mobile-card-row"><span class="k">餐别</span><span class="v">{{ row.bcType }}</span></div>
      </div>
    </div>

    <!-- 桌面端: 表格 -->
    <el-table v-else :data="rows" stripe v-loading="loading" border>
      <el-table-column prop="dinTime" label="报餐时间" min-width="170" />
      <el-table-column prop="name" label="姓名" min-width="100" />
      <el-table-column prop="mobile" label="手机号" min-width="130" />
      <el-table-column prop="deptName" label="部门" min-width="120" />
      <el-table-column prop="bcType" label="餐别" width="90" />
      <el-table-column prop="bcChannel" label="报餐方式" width="110" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.bcChannel === '预约报餐' ? 'warning' : 'success'" effect="plain">
            {{ row.bcChannel === '预约报餐' ? '预约' : '已报' }}
          </el-tag>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!loading && !rows.length" description="暂无报餐记录" />

    <div class="pager">
      <el-pagination :layout="isMobile ? 'prev, pager, next' : 'total, sizes, prev, pager, next'" :total="total"
        :current-page="page" :page-size="size" :page-sizes="[10, 20, 50]" @current-change="onPage"
        @size-change="onSize" />
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Refresh, Download } from '@element-plus/icons-vue'
import { getRecordList, exportRecords, getDepartmentList } from '../api/bc'
import { saveBlob } from '../utils/download'
import { useIsMobile } from '../composables/useIsMobile'

const { isMobile } = useIsMobile()
const loading = ref(false)
const exporting = ref(false)
const rows = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const depts = ref([])
const dateRange = ref([])
const q = reactive({ name: '', mobile: '', departmentId: null })

const params = computed(() => {
  const p = {
    currentPage: page.value,
    pageSize: size.value,
    name: q.name || undefined,
    mobile: q.mobile || undefined,
    departmentId: q.departmentId || undefined
  }
  if (dateRange.value && dateRange.value.length === 2) {
    p.startTime = dateRange.value[0] + ' 00:00:00'
    p.endTime = dateRange.value[1] + ' 23:59:59'
  }
  return p
})

function parseTotal(t) {
  // 后端 total 形如 [{count: N}]
  if (Array.isArray(t)) {
    if (!t.length) return 0
    const obj = t[0]
    const k = Object.keys(obj)[0]
    return Number(obj[k]) || 0
  }
  return Number(t) || 0
}

async function load() {
  loading.value = true
  try {
    const res = await getRecordList(params.value)
    rows.value = (res && res.data) || []
    total.value = parseTotal(res && res.total)
  } finally {
    loading.value = false
  }
}
function search() {
  page.value = 1
  load()
}
function reset() {
  q.name = ''
  q.mobile = ''
  q.departmentId = null
  dateRange.value = []
  search()
}
function onPage(p) {
  page.value = p
  load()
}
function onSize(s) {
  size.value = s
  page.value = 1
  load()
}
async function onExport() {
  exporting.value = true
  try {
    const resp = await exportRecords({
      name: q.name || undefined,
      mobile: q.mobile || undefined,
      departmentId: q.departmentId || undefined,
      startTime: params.value.startTime,
      endTime: params.value.endTime
    })
    saveBlob(resp, '报餐记录.xls')
    ElMessage.success('导出成功')
  } catch (e) {
    /* 已提示 */
  } finally {
    exporting.value = false
  }
}

onMounted(async () => {
  depts.value = (await getDepartmentList()) || []
  load()
})
</script>
