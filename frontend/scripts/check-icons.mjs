// 校验源码里用到的 Element Plus 图标名全部真实存在
// （图标是通过 <component :is="'Name'"> 动态渲染的，写错名字构建不会报错，只会在运行时空白）
import fs from 'node:fs'
import path from 'node:path'
import { createRequire } from 'node:module'

const require = createRequire(import.meta.url)
const icons = require('@element-plus/icons-vue')

const srcDir = path.join(process.cwd(), 'src')

// 只收集「动态图标」场景：:is="..." 里的字符串、meta.icon 配置、以及 icon: 'Xxx' 字段
const dynamicPatterns = [
  /:is="\s*'([A-Za-z0-9]+)'\s*"/g, // :is="'Shop'"
  /icon:\s*'([A-Za-z0-9]+)'/g, // meta: { icon: 'Odometer' }
  /icon="([A-Za-z0-9]+)"/g, // <el-icon icon="Refresh"> 之类
  /:icon="([A-Za-z0-9]+)"/g // :icon="Plus"
]

// 组件式图标：<el-icon><Shop /></el-icon>、<Plus />
const componentPattern = /<([A-Z][A-Za-z0-9]*)\s*\/>/g

const files = []
function walk(dir) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name)
    if (entry.isDirectory()) walk(full)
    else if (/\.(vue|js)$/.test(entry.name)) files.push(full)
  }
}
walk(srcDir)

const namedImports = new Set()
const usedDynamic = new Map() // name -> [files]
const usedComponent = new Map()
const usedBindings = new Map() // :icon="Xxx" 需要在本文件内 import 或全局注册

for (const file of files) {
  const rel = path.relative(process.cwd(), file)
  const text = fs.readFileSync(file, 'utf8')

  // 显式 import 的图标（如 import { Search, Plus } from '@element-plus/icons-vue'）
  const fileImports = new Set()
  const importBlocks = text.match(/import\s*\{([^}]*)\}\s*from\s*['"]@element-plus\/icons-vue['"]/g) || []
  for (const block of importBlocks) {
    const inner = block.match(/\{([^}]*)\}/)[1]
    inner
      .split(',')
      .map((s) => s.trim())
      .filter(Boolean)
      .forEach((n) => {
        namedImports.add(n)
        fileImports.add(n)
      })
  }

  for (const re of dynamicPatterns) {
    re.lastIndex = 0
    let m
    while ((m = re.exec(text))) {
      const name = m[1]
      if (!/^[A-Z]/.test(name)) continue // 小写值（如 el-result 的 icon="warning"）不是组件名
      if (!usedDynamic.has(name)) usedDynamic.set(name, [])
      usedDynamic.get(name).push(rel)
    }
  }

  componentPattern.lastIndex = 0
  let m
  while ((m = componentPattern.exec(text))) {
    const name = m[1]
    if (!usedComponent.has(name)) usedComponent.set(name, [])
    usedComponent.get(name).push(rel)
  }

  // 校验 :icon="Xxx" 形式的绑定：必须是本文件 import 的图标
  const bindRe = /:icon="([A-Za-z0-9_]+)"/g
  let b
  while ((b = bindRe.exec(text))) {
    const name = b[1]
    if (!fileImports.has(name)) {
      if (!usedBindings.has(name)) usedBindings.set(name, [])
      usedBindings.get(name).push(rel + ' (未 import)')
    }
  }
}

// 「自身/自定义组件」白名单：不是 Element Plus 图标
const customComponents = new Set(['App', 'RouterView', 'component', 'Shop', 'Transition'])
  // Shop 既是图标也是自定义组件名，统一按图标校验

const missingDynamic = [...usedDynamic.keys()].filter((n) => !(n in icons))
const missingComponent = [...usedComponent.keys()].filter(
  (n) => !(n in icons) && !customComponents.has(n) && !n.endsWith('View')
)
const missingNamed = [...namedImports].filter((n) => !(n in icons))

console.log('图标总数（@element-plus/icons-vue）:', Object.keys(icons).length)
console.log('')
console.log('动态使用（:is / meta.icon / icon 属性）:', [...usedDynamic.keys()].sort().join(', '))
console.log('模板中直接使用的图标组件:', [...usedComponent.keys()].sort().join(', '))
console.log('显式 import 的图标:', [...namedImports].sort().join(', '))
console.log('')

let bad = 0
if (missingDynamic.length) {
  bad += missingDynamic.length
  console.log('❌ 不存在的动态图标名:')
  missingDynamic.forEach((n) => console.log('   ' + n + '  ← ' + usedDynamic.get(n).join(', ')))
}
if (missingComponent.length) {
  bad += missingComponent.length
  console.log('❌ 不存在/未定义的图标组件:')
  missingComponent.forEach((n) => console.log('   ' + n + '  ← ' + usedComponent.get(n).join(', ')))
}
if (missingNamed.length) {
  bad += missingNamed.length
  console.log('❌ import 了不存在的图标:')
  missingNamed.forEach((n) => console.log('   ' + n))
}
if (usedBindings.size) {
  bad += usedBindings.size
  console.log('❌ :icon 绑定未在本文件 import:')
  usedBindings.forEach((where, name) => console.log('   ' + name + '  ← ' + where.join(', ')))
}
if (bad === 0) console.log('✅ 全部图标名有效，且 :icon 绑定均已 import')
process.exit(bad === 0 ? 0 : 1)
