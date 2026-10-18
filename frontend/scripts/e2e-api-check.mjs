// 端到端接口自检：通过 Vite 代理（/api）真实调用后端，验证契约与关键页面数据源。
const BASE = process.argv[2] || 'http://127.0.0.1:5173'
const API = BASE + '/api'

let pass = 0
let fail = 0
const failures = []

function log(ok, label, extra = '') {
  if (ok) {
    pass += 1
    console.log('OK   ' + label + (extra ? '  ' + extra : ''))
  } else {
    fail += 1
    failures.push(label + ' ' + extra)
    console.log('FAIL ' + label + '  ' + extra)
  }
}

async function call(method, path, { token, body, raw } = {}) {
  const headers = {}
  if (token) headers['Authorization'] = 'Bearer ' + token
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  const res = await fetch(API + path, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body)
  })
  const text = await res.text()
  let json = null
  try {
    json = JSON.parse(text)
  } catch (e) {
    /* 非 JSON */
  }
  return raw ? { status: res.status, text, json } : { status: res.status, json }
}

/* ---------- 1. 登录 ---------- */
const logins = [
  ['admin', 'admin123'],
  ['product', 'product123'],
  ['viewer', 'viewer123']
]
let adminToken = ''
for (const [u, p] of logins) {
  const r = await call('POST', '/auth/login', { body: { username: u, password: p } })
  const ok = r.status === 200 && r.json && r.json.code === 200 && r.json.data && r.json.data.token
  log(ok, `POST /auth/login (${u})`, ok ? `code=${r.json.code} 权限=${(r.json.data.userInfo.permissions || []).length} 个` : JSON.stringify(r.json).slice(0, 200))
  if (u === 'admin' && ok) adminToken = r.json.data.token
}

if (!adminToken) {
  console.log('\n登录失败，后续需要 token 的用例跳过')
  console.log(`\n通过 ${pass} / 失败 ${fail}`)
  process.exit(1)
}

/* ---------- 2. 当前用户 / 在线会话 ---------- */
const me = await call('GET', '/auth/me', { token: adminToken })
log(me.json?.code === 200 && !!me.json.data?.username, 'GET /auth/me', 'username=' + me.json?.data?.username)

const online = await call('GET', '/auth/online', { token: adminToken })
log(online.json?.code === 200, 'GET /auth/online', 'onlineSessions=' + JSON.stringify(online.json?.data))

/* ---------- 3. 分类 / 品牌 ---------- */
const cats = await call('GET', '/categories/all', { token: adminToken })
const catList = Array.isArray(cats.json?.data) ? cats.json.data : cats.json?.data?.records || []
log(cats.json?.code === 200, 'GET /categories/all', '共 ' + catList.length + ' 条')
log(catList.length === 0 || 'productCount' in catList[0], '分类字段 productCount', '示例=' + JSON.stringify(catList[0] || {}).slice(0, 160))

const brands = await call('GET', '/products/brands', { token: adminToken })
log(brands.json?.code === 200, 'GET /products/brands', '共 ' + (brands.json?.data || []).length + ' 个品牌')

/* ---------- 4. 商品分页 ---------- */
const products = await call('GET', '/products?page=1&size=5', { token: adminToken })
const pdata = products.json?.data
log(products.json?.code === 200 && !!pdata?.records, 'GET /products', `total=${pdata?.total} size=${pdata?.size} pages=${pdata?.pages}`)

/* ---------- 5. 全文检索 + 高亮 ---------- */
const search = await call('POST', '/search/products', {
  token: adminToken,
  body: { keyword: '', page: 1, size: 5, sortField: '_score', sortOrder: 'desc' }
})
const sdata = search.json?.data
const srec = (sdata?.records || [])[0]
log(search.json?.code === 200, 'POST /search/products', `total=${sdata?.total}`)
log(!!srec && 'nameHighlight' in srec, '检索结果含 nameHighlight 字段', '示例=' + JSON.stringify(srec?.nameHighlight || '').slice(0, 120))
log(!!srec && ('score' in srec), '检索结果含 score 字段', 'score=' + srec?.score)

const kw = '手机'
const searchKw = await call('POST', '/search/products', {
  token: adminToken,
  body: { keyword: kw, page: 1, size: 5, sortField: '_score', sortOrder: 'desc' }
})
const kwRecs = searchKw.json?.data?.records || []
const hlHit = kwRecs.find((r) => (r.nameHighlight || '').includes('<em class="hl">'))
log(
  searchKw.json?.code === 200,
  `关键词检索 "${kw}"`,
  `命中 ${searchKw.json?.data?.total} 条`
)
log(
  !!hlHit || kwRecs.length === 0,
  '高亮标签 <em class="hl"> 由后端返回',
  hlHit ? '示例=' + hlHit.nameHighlight : '（该关键词无命中，跳过）'
)

const sstatus = await call('GET', '/search/status', { token: adminToken })
log(sstatus.json?.code === 200, 'GET /search/status', JSON.stringify(sstatus.json?.data?.indexStatus))

/* ---------- 6. 索引任务 ---------- */
const tasks = await call('GET', '/index-tasks?page=1&size=5', { token: adminToken })
log(tasks.json?.code === 200, 'GET /index-tasks', '共 ' + (tasks.json?.data?.total ?? 0) + ' 条任务')

const overview = await call('GET', '/index-tasks/overview', { token: adminToken })
log(overview.json?.code === 200, 'GET /index-tasks/overview', '队列=' + overview.json?.data?.queue)

/* ---------- 7. 用户 / 角色 / 权限 / 日志 ---------- */
const users = await call('GET', '/users?page=1&size=5', { token: adminToken })
const uRec = (users.json?.data?.records || [])[0]
log(users.json?.code === 200, 'GET /users', 'total=' + users.json?.data?.total)
log(!!uRec && Array.isArray(uRec.roles), '用户对象含 roles 数组', '示例=' + JSON.stringify(uRec?.roles))

const roles = await call('GET', '/roles/all', { token: adminToken })
const rRec = (roles.json?.data || [])[0]
log(roles.json?.code === 200, 'GET /roles/all', '共 ' + (roles.json?.data || []).length + ' 个角色')
log(!!rRec && 'permissionIds' in rRec, '角色对象含 permissionIds', '示例=' + JSON.stringify(rRec?.permissionIds || []).slice(0, 100))

const ptree = await call('GET', '/permissions/tree', { token: adminToken })
const tree = ptree.json?.data || []
log(ptree.json?.code === 200 && tree.length > 0, 'GET /permissions/tree', '顶级节点 ' + tree.length + ' 个，子节点=' + (tree[0]?.children?.length ?? 0))
log(!!tree[0] && 'id' in tree[0] && 'name' in tree[0], '权限树节点含 id/name', '示例=' + JSON.stringify({ id: tree[0]?.id, name: tree[0]?.name, type: tree[0]?.type }))

const perms = await call('GET', '/permissions?page=1&size=5', { token: adminToken })
log(perms.json?.code === 200, 'GET /permissions', 'total=' + perms.json?.data?.total)

const logs = await call('GET', '/operation-logs?page=1&size=5', { token: adminToken })
const lRec = (logs.json?.data?.records || [])[0]
log(logs.json?.code === 200, 'GET /operation-logs', 'total=' + logs.json?.data?.total)
log(!!lRec && 'params' in lRec && 'success' in lRec, '日志对象含 params/success', '示例=' + JSON.stringify({ ip: lRec?.ip, durationMs: lRec?.durationMs, success: lRec?.success }))

const modules = await call('GET', '/operation-logs/modules', { token: adminToken })
log(modules.json?.code === 200, 'GET /operation-logs/modules', JSON.stringify(modules.json?.data))

/* ---------- 8. 只读账号权限校验（403 场景） ---------- */
const vLogin = await call('POST', '/auth/login', { body: { username: 'viewer', password: 'viewer123' } })
const viewerToken = vLogin.json?.data?.token
const viewerPerms = vLogin.json?.data?.userInfo?.permissions || []
log(!!viewerToken, 'viewer 登录', '权限=' + JSON.stringify(viewerPerms))
const forbidden = await call('POST', '/products', {
  token: viewerToken,
  body: { name: 'x', code: 'x-1', categoryId: 1, price: 1, stock: 1, status: 1 }
})
log(forbidden.json?.code === 403, 'viewer 新增商品被拒绝', 'code=' + forbidden.json?.code + ' msg=' + forbidden.json?.message)

/* ---------- 9. 退出登录使 token 失效 ---------- */
const out = await call('POST', '/auth/logout', { token: adminToken })
log(out.json?.code === 200, 'POST /auth/logout', 'code=' + out.json?.code)
const after = await call('GET', '/auth/me', { token: adminToken })
log(after.json?.code === 401, '退出后 token 立即失效', 'code=' + after.json?.code + ' msg=' + after.json?.message)

console.log('')
console.log(`用例通过 ${pass} 个，失败 ${fail} 个`)
if (failures.length) {
  console.log('失败明细：')
  failures.forEach((f) => console.log('  - ' + f))
}
process.exit(fail === 0 ? 0 : 1)
