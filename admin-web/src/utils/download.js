// 把 axios blob 响应保存为文件
export function saveBlob(resp, fallbackName) {
  const blob = new Blob([resp.data])
  let filename = fallbackName || 'export.xls'
  const disp = resp.headers && resp.headers['content-disposition']
  if (disp) {
    const m = /filename\*?=(?:UTF-8'')?["']?([^"';]+)/i.exec(disp)
    if (m) filename = decodeURIComponent(m[1])
  }
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  window.URL.revokeObjectURL(url)
}
