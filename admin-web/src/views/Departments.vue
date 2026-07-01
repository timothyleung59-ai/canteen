<template>
  <div class="page-card">
    <div class="toolbar">
      <span style="font-weight: 600">部门列表</span>
      <div class="grow"></div>
      <el-button type="primary" :icon="Plus" @click="openAdd">新增部门</el-button>
    </div>

    <!-- 手机端: 卡片列表 -->
    <div v-if="isMobile" v-loading="loading">
      <div v-for="row in rows" :key="row.id" class="mobile-card">
        <div class="mobile-card-header">
          <span class="mobile-card-title">{{ row.name }}</span>
          <el-tag type="info" effect="plain">{{ row.headcount != null ? row.headcount : '-' }} 人</el-tag>
        </div>
        <div class="mobile-card-actions">
          <el-button size="small" :icon="Edit" @click="openEdit(row)">重命名</el-button>
          <el-button size="small" type="danger" :icon="Delete" @click="remove(row)">删除</el-button>
        </div>
      </div>
    </div>

    <!-- 桌面端: 表格 -->
    <el-table v-else :data="rows" stripe v-loading="loading" border>
      <el-table-column type="index" label="#" width="60" />
      <el-table-column prop="name" label="部门名称" min-width="200" />
      <el-table-column prop="headcount" label="员工人数" width="140">
        <template #default="{ row }">{{ row.headcount != null ? row.headcount : '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button size="small" :icon="Edit" @click="openEdit(row)">重命名</el-button>
          <el-button size="small" type="danger" :icon="Delete" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!loading && !rows.length" description="暂无部门，点击右上角新增" />

    <el-dialog v-model="dlg" :title="editing ? '重命名部门' : '新增部门'" width="360px">
      <el-form @submit.prevent="save">
        <el-form-item>
          <el-input v-model="nameInput" placeholder="请输入部门名称" maxlength="20" show-word-limit
            @keyup.enter="save" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete } from '@element-plus/icons-vue'
import {
  getDepartmentPageList,
  saveDepartment,
  updateDepartmentName,
  deleteDepartment
} from '../api/bc'
import { useIsMobile } from '../composables/useIsMobile'

const { isMobile } = useIsMobile()
const loading = ref(false)
const rows = ref([])
const dlg = ref(false)
const editing = ref(null)
const nameInput = ref('')
const saving = ref(false)

async function load() {
  loading.value = true
  try {
    const res = await getDepartmentPageList(1, 200)
    rows.value = (res && res.data) || []
  } finally {
    loading.value = false
  }
}

function openAdd() {
  editing.value = null
  nameInput.value = ''
  dlg.value = true
}
function openEdit(row) {
  editing.value = row
  nameInput.value = row.name
  dlg.value = true
}
async function save() {
  const name = nameInput.value.trim()
  if (!name) {
    ElMessage.warning('请输入部门名称')
    return
  }
  saving.value = true
  try {
    if (editing.value) {
      await updateDepartmentName(editing.value.id, name)
      ElMessage.success('修改成功')
    } else {
      await saveDepartment(name)
      ElMessage.success('新增成功')
    }
    dlg.value = false
    load()
  } finally {
    saving.value = false
  }
}
async function remove(row) {
  await ElMessageBox.confirm(
    `确定删除部门「${row.name}」？该部门下员工将变为未分配。`,
    '提示',
    { type: 'warning' }
  ).catch(() => Promise.reject())
  await deleteDepartment(row.id)
  ElMessage.success('删除成功')
  load()
}

onMounted(load)
</script>
