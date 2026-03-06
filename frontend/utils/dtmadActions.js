// Proxied through Next.js rewrites (next.config.mjs) to avoid CORS
const DTM_URL = ''
const AD_URL = ''

async function fetchJSON(baseUrl, path) {
  const res = await fetch(`${baseUrl}${path}`)
  if (!res.ok) throw new Error(`API error: ${res.status} ${res.statusText}`)
  return res.json()
}

async function postJSON(baseUrl, path, body) {
  const res = await fetch(`${baseUrl}${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
  if (!res.ok) throw new Error(`API error: ${res.status} ${res.statusText}`)
  return res.json()
}

// DTM Endpoints
export const fetchDTMInstances = () =>
  fetchJSON(DTM_URL, '/sphinx/dtm/instance/all')

export const toggleDTMInstance = (id) =>
  fetchJSON(DTM_URL, `/sphinx/dtm/instance/toggle/${id}`)

export const deleteDTMInstance = (id) =>
  fetchJSON(DTM_URL, `/sphinx/dtm/instance/delete/${id}`)

export const saveInstance = (model) =>
  postJSON(DTM_URL, '/sphinx/dtm/instance/save', model)

export const saveAsset = (model) =>
  postJSON(DTM_URL, '/sphinx/dtm/assetcatalogue/save', model)

export const deleteAsset = (id) =>
  fetchJSON(DTM_URL, `/sphinx/dtm/assetcatalogue/delete/${id}`)

export const fetchDTMAlerts = (pageNo = 0, pageSize = 20) =>
  fetchJSON(DTM_URL, `/sphinx/dtm/alert/all/DTM?pageNo=${pageNo}&pageSize=${pageSize}`)

export const fetchTsharkProcesses = (instanceId) =>
  fetchJSON(DTM_URL, `/sphinx/dtm/tshark/getProcesses/${instanceId}`)

export const startTsharkProcess = (processId, instanceId) =>
  fetchJSON(DTM_URL, `/sphinx/dtm/tshark/start/${processId}/${instanceId}`)

export const stopTsharkProcess = (processId, instanceId) =>
  fetchJSON(DTM_URL, `/sphinx/dtm/tshark/stop/${processId}/${instanceId}`)

export const fetchTsharkStatus = (processId, instanceId) =>
  fetchJSON(DTM_URL, `/sphinx/dtm/tshark/status/${processId}/${instanceId}`)

export const enableTsharkProcess = (processId, instanceId) =>
  fetchJSON(DTM_URL, `/sphinx/dtm/tshark/enable/${processId}/${instanceId}`)

export const disableTsharkProcess = (processId, instanceId) =>
  fetchJSON(DTM_URL, `/sphinx/dtm/tshark/disable/${processId}/${instanceId}`)

export const fetchAssetCatalogue = () =>
  fetchJSON(DTM_URL, '/sphinx/dtm/assetcatalogue/getAssetCatalogueList')

export const fetchAssetDiscoveryAlerts = () =>
  fetchJSON(DTM_URL, '/sphinx/dtm/assetcatalogue/getAssetDiscoveryAlerts')

export const fetchDTMStatistics = () =>
  fetchJSON(DTM_URL, '/sphinx/dtm/statistics')

export const fetchSuricataStatistics = () =>
  fetchJSON(DTM_URL, '/sphinx/dtm/suricata/statistics')

export const fetchSuricataDecoderStats = () =>
  fetchJSON(DTM_URL, '/sphinx/dtm/suricata/statistics/getDecoderStatistics')

export const fetchSuricataPerInstanceStats = () =>
  fetchJSON(DTM_URL, '/sphinx/dtm/suricata/statistics/getDecoderPerInstanceStatistics')

export const fetchDTMConfig = () =>
  fetchJSON(DTM_URL, '/sphinx/dtm/config/all')

export const fetchPortCatalogue = () =>
  fetchJSON(DTM_URL, '/sphinx/dtm/portcatalogue/getPortCatalogueList')

// AD Endpoints
export const fetchADAlgorithmList = () =>
  fetchJSON(AD_URL, '/sphinx/ad/config/algorithmList')

export const fetchADAlgorithmProperties = () =>
  fetchJSON(AD_URL, '/sphinx/ad/config/algorithmProperties')

export const fetchADConfig = (prefix) =>
  fetchJSON(AD_URL, `/sphinx/ad/config/all/${prefix}`)

export const fetchADAllConfig = () =>
  fetchJSON(AD_URL, '/sphinx/ad/config/all')

export async function saveADConfig(configModel) {
  const res = await fetch(`${AD_URL}/sphinx/ad/config/save`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(configModel),
  })
  if (!res.ok) throw new Error(`AD config save failed: ${res.status}`)
  return res.json()
}

export const fetchADAlerts = (date, limit = 50) =>
  fetchJSON(AD_URL, `/sphinx/ad/alert/getAlerts?date=${date}&limit=${limit}`)

export const fetchADSimulations = () =>
  fetchJSON(AD_URL, '/sphinx/ad/simulation/getAlgorithmSimulationList')

export const executeADSimulation = (filename) =>
  fetchJSON(AD_URL, `/sphinx/ad/simulation/execute/${filename}`)

export const fetchADComponents = () =>
  fetchJSON(AD_URL, '/sphinx/ad/component/list')
