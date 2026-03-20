import axios from 'axios'

const apiBasePath = import.meta.env.VITE_API_BASE_PATH || '/api'

const api = axios.create({
  baseURL: apiBasePath,
  timeout: 15000
})

api.interceptors.response.use((response) => {
  const payload = response.data

  // Unified API envelope:
  // - SuccessResponse: { success, message, data, error, timestamp }
  // - PagingResponse:  { success, message, data, error, timestamp, pagination }
  if (
    payload &&
    typeof payload === 'object' &&
    Object.prototype.hasOwnProperty.call(payload, 'success') &&
    Object.prototype.hasOwnProperty.call(payload, 'data')
  ) {
    if (Object.prototype.hasOwnProperty.call(payload, 'pagination')) {
      return payload
    }
    return payload.data
  }

  return payload
})

// ── Transactions ──────────────────────────────────────────────
export const transactionApi = {
  list(params) {
    return api.get('/transactions', { params })
  },
  create(data) {
    return api.post('/transactions', data)
  },
  update(id, data) {
    return api.put(`/transactions/${id}`, data)
  },
  importExcel(accountId, file) {
    const form = new FormData()
    form.append('file', file)
    form.append('accountId', accountId)
    return api.post('/transactions/import', form, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  importStockHistory(accountId, file) {
    const form = new FormData()
    form.append('file', file)
    form.append('accountId', accountId)
    return api.post('/transactions/import/stock-history', form, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  importFundHistory(accountId, file) {
    const form = new FormData()
    form.append('file', file)
    form.append('accountId', accountId)
    return api.post('/transactions/import/fund-history', form, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  importStockStatement(accountId, file) {
    const form = new FormData()
    form.append('file', file)
    form.append('accountId', accountId)
    return api.post('/transactions/import/stock-statement', form, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  }
}

// ── Cash Flows ────────────────────────────────────────────────
export const cashFlowApi = {
  list(accountId) {
    return api.get('/cash-flows', { params: { accountId } })
  },
  create(data) {
    return api.post('/cash-flows', data)
  },
  importExcel(accountId, file) {
    const form = new FormData()
    form.append('file', file)
    form.append('accountId', accountId)
    return api.post('/cash-flows/import', form, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  importCashStatement(accountId, file) {
    const form = new FormData()
    form.append('file', file)
    form.append('accountId', accountId)
    return api.post('/cash-flows/import/cash-statement', form, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  }
}

// ── Analytics ─────────────────────────────────────────────────
export const analyticsApi = {
  portfolio(accountId) {
    return api.get(`/analytics/portfolio/${accountId}`)
  },
  cashSummary(accountId) {
    return api.get(`/analytics/cash/${accountId}`)
  }
}

// ── Prices ────────────────────────────────────────────────────
export const priceApi = {
  update(data) {
    return api.post('/prices', data)
  }
}

// ── Reference Data ────────────────────────────────────────────
export const refApi = {
  getTickers()          { return api.get('/ref/tickers') },
  createTicker(data)    { return api.post('/ref/tickers', data) },
  updateTicker(id, data){ return api.put(`/ref/tickers/${id}`, data) },

  getBrokers()          { return api.get('/ref/brokers') },
  createBroker(data)    { return api.post('/ref/brokers', data) },
  updateBroker(id, data){ return api.put(`/ref/brokers/${id}`, data) },

  getAccounts()         { return api.get('/ref/accounts') },
  createAccount(data)   { return api.post('/ref/accounts', data) },
  updateAccount(id, data){ return api.put(`/ref/accounts/${id}`, data) }
}

export default api
