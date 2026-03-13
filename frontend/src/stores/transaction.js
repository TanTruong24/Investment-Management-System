import { defineStore } from 'pinia'
import { ref } from 'vue'
import { transactionApi, refApi } from '@/services/api'

export const useTransactionStore = defineStore('transaction', () => {
  const transactions = ref([])
  const total = ref(0)
  const loading = ref(false)

  const accounts = ref([])
  const tickers = ref([])

  async function fetchTransactions(params = {}) {
    loading.value = true
    try {
      const res = await transactionApi.list(params)
      transactions.value = res.content
      total.value = res.totalElements
    } finally {
      loading.value = false
    }
  }

  async function createTransaction(data) {
    return await transactionApi.create(data)
  }

  async function updateTransaction(id, data) {
    return await transactionApi.update(id, data)
  }

  async function importExcel(accountId, file) {
    return await transactionApi.importExcel(accountId, file)
  }

  async function importStockHistory(accountId, file) {
    return await transactionApi.importStockHistory(accountId, file)
  }

  async function importFundHistory(accountId, file) {
    return await transactionApi.importFundHistory(accountId, file)
  }

  async function importStockStatement(accountId, file) {
    return await transactionApi.importStockStatement(accountId, file)
  }

  async function loadRefData() {
    const [accRes, tickRes] = await Promise.all([
      refApi.getAccounts(),
      refApi.getTickers()
    ])
    accounts.value = accRes
    tickers.value = tickRes
  }

  return { transactions, total, loading, accounts, tickers,
           fetchTransactions, createTransaction, updateTransaction,
           importExcel, importStockHistory, importFundHistory, importStockStatement, loadRefData }
})
