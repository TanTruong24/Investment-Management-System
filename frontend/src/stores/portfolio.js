import { defineStore } from 'pinia'
import { ref } from 'vue'
import { analyticsApi, cashFlowApi } from '@/services/api'

export const usePortfolioStore = defineStore('portfolio', () => {
  const summary = ref(null)
  const cashSummary = ref(null)
  const cashFlows = ref([])
  const loading = ref(false)

  async function fetchPortfolio(accountId) {
    loading.value = true
    try {
      summary.value = await analyticsApi.portfolio(accountId)
    } finally {
      loading.value = false
    }
  }

  async function fetchCashSummary(accountId) {
    cashSummary.value = await analyticsApi.cashSummary(accountId)
  }

  async function fetchCashFlows(accountId) {
    cashFlows.value = await cashFlowApi.list(accountId)
  }

  async function addCashFlow(data) {
    return await cashFlowApi.create(data)
  }

  async function importCashFlows(accountId, file) {
    return await cashFlowApi.importExcel(accountId, file)
  }

  async function importCashStatement(accountId, file) {
    return await cashFlowApi.importCashStatement(accountId, file)
  }

  return { summary, cashSummary, cashFlows, loading,
           fetchPortfolio, fetchCashSummary, fetchCashFlows, addCashFlow, importCashFlows, importCashStatement }
})
