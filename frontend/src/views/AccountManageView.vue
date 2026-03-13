<template>
  <div>
    <div class="toolbar">
      <el-button type="primary" @click="openDialog()">
        <el-icon><Plus /></el-icon> Thêm tài khoản
      </el-button>
    </div>

    <el-card shadow="never">
      <el-table :data="accts" stripe border v-loading="loading">
        <el-table-column prop="accountNumber" label="Số TK"   width="140" />
        <el-table-column prop="name"          label="Tên TK"  min-width="160" />
        <el-table-column prop="brokerCode"    label="Broker"  width="90" />
        <el-table-column prop="cashBalance"   label="Số dư TM" width="150" align="right"
          :formatter="(r) => formatVND(r.cashBalance)" />
        <el-table-column prop="purchasingPower" label="Sức mua" width="150" align="right"
          :formatter="(r) => formatVND(r.purchasingPower)" />
        <el-table-column prop="active" label="Kích hoạt" width="100">
          <template #default="{ row }">
            <el-tag :type="row.active ? 'success' : 'info'">{{ row.active ? 'Có' : 'Không' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Thao tác" width="90" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDialog(row)">Sửa</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingRow ? 'Sửa tài khoản' : 'Thêm tài khoản'" width="460px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="140px">
        <el-form-item label="Số tài khoản" prop="accountNumber">
          <el-input v-model="form.accountNumber" />
        </el-form-item>
        <el-form-item label="Tên tài khoản" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="Broker" prop="brokerId">
          <el-select v-model="form.brokerId" style="width:100%">
            <el-option v-for="b in brokers" :key="b.id" :label="`${b.code} – ${b.name}`" :value="b.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="Kích hoạt">
          <el-switch v-model="form.active" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">Hủy</el-button>
        <el-button type="primary" :loading="saving" @click="save">Lưu</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { refApi } from '@/services/api'
import { ElMessage } from 'element-plus'

const accts   = ref([])
const brokers = ref([])
const loading = ref(false)
const saving  = ref(false)
const dialogVisible = ref(false)
const editingRow = ref(null)
const formRef = ref(null)

const form = ref({ accountNumber: '', name: '', brokerId: null, active: true })
const rules = {
  accountNumber: [{ required: true, message: 'Nhập số tài khoản' }],
  name:          [{ required: true, message: 'Nhập tên tài khoản' }],
  brokerId:      [{ required: true, message: 'Chọn broker' }]
}

onMounted(async () => {
  loading.value = true
  try {
    const [accountData, brokerData] = await Promise.all([refApi.getAccounts(), refApi.getBrokers()])
    brokers.value = brokerData
    accts.value = mapAccounts(accountData)
  } finally {
    loading.value = false
  }
})

function openDialog(row = null) {
  editingRow.value = row
  form.value = row
    ? { accountNumber: row.accountNumber, name: row.name, brokerId: row.brokerId ?? row.broker?.id ?? null, active: row.active }
    : { accountNumber: '', name: '', brokerId: null, active: true }
  dialogVisible.value = true
}

async function save() {
  await formRef.value.validate()

  const payload = {
    accountNumber: form.value.accountNumber?.trim(),
    name: form.value.name?.trim(),
    broker: { id: form.value.brokerId },
    active: form.value.active
  }

  saving.value = true
  try {
    if (editingRow.value) {
      await refApi.updateAccount(editingRow.value.id, payload)
      ElMessage.success('Cập nhật thành công')
    } else {
      await refApi.createAccount(payload)
      ElMessage.success('Thêm thành công')
    }
    dialogVisible.value = false
    accts.value = mapAccounts(await refApi.getAccounts())
  } catch (e) {
    ElMessage.error(e.response?.data?.message || 'Có lỗi xảy ra')
  } finally {
    saving.value = false
  }
}

function mapAccounts(rows) {
  return (rows || []).map(a => ({
    ...a,
    brokerId: a.broker?.id ?? null,
    brokerCode: a.broker?.code ?? '—'
  }))
}

function formatVND(val) {
  if (val == null) return '—'
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val)
}
</script>

<style scoped>
.toolbar { margin-bottom: 12px; }
</style>
