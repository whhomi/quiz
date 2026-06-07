# Cloudflare Pages 部署指南 — 刷题助手

> 刷题助手是一个纯前端 Vue 3 + Vite SPA，零后端依赖，适合部署到 Cloudflare Pages（永久免费套餐足够用）。

---

## 1. 为什么选 Cloudflare Pages

| 特性 | 说明 |
|------|------|
| 免费额度 | 每月无限请求 / 带宽 100GB / 1 次构建并发 / 500 次构建 |
| 全球 CDN | 330+ 节点，国内访问延迟 ~50-100ms |
| 自动 HTTPS | 免费 SSL 证书，自动续期 |
| Git 集成 | 推代码自动构建部署 |
| 自定义域名 | 免费绑定，自动 SSL |
| 预览环境 | 每个 PR 自动生成预览链接 |
| Cloudflare Workers | 可搭配 Workers 做 API 代理/重定向（本项目不需要） |

---

## 2. 项目 Cloudflare 配置（已就绪）

### 2.1 SPA 路由 — 不需要 `_redirects`

刷题助手使用 Vue Router Hash 模式 (`createWebHashHistory`)，所有路由通过 URL hash 实现（如 `/#/quiz`）。hash 部分不会发送到服务器，服务器始终只收到 `/` 或 `/index.html` 请求，因此**不需要** SPA fallback 的 `_redirects` 规则。

如果添加 `/* /index.html 200` 反而会在 Workers 部署时触发 Cloudflare 的无限循环检测报错。（Pages 部署则有内置的 SPA 兼容处理，即使不加也没问题。）

### 2.2 `public/_headers` — 静态资源长缓存

```
/assets/*
  Cache-Control: public, max-age=31536000, immutable
```

Vite 构建的 JS/CSS 文件名带内容哈希，设置 `immutable` + 一年缓存是最佳实践：

- `dist/assets/excel-worker-XXXXXX.js` — Worker 独立 chunk，约 335KB
- `dist/assets/index-XXXXXX.css` — Tailwind 样式，约 23KB  
- `dist/assets/index-XXXXXX.js` — 主应用 + Vue/Router/xlsx，约 469KB
- 文件名变化 = 内容变化，永远不会缓存到过期版本

---

## 3. 部署方式

### 方式 A：Git 仓库自动部署（推荐）

**Step 1**：初始化 Git 仓库并推送

```bash
cd /root/hermes-webui/hermes_data/.hermes/mywork
git init
git add .
git commit -m "刷题助手初始版本"
git remote add origin https://github.com/你的用户名/quiz-helper.git
git push -u origin main
```

**Step 2**：登录 Cloudflare Dashboard

打开 https://dash.cloudflare.com/ → Workers & Pages → Pages → 连接到 Git

**Step 3**：配置构建设置

| 配置项 | 值 |
|--------|-----|
| 框架预设 | Vite |
| 构建命令 | `npm run build` |
| 构建输出目录 | `dist` |
| 根目录 | `/` (默认为空) |
| 环境变量 | 不需要 |

**Step 4**：点击「保存并部署」

首次部署约 30-60 秒。之后每次 `git push` 自动触发重新部署。

---

### 方式 B：wrangler CLI 手动部署

**Step 1**：安装 wrangler

```bash
npm install -g wrangler
```

**Step 2**：登录

```bash
wrangler login
```

**Step 3**：本地构建

```bash
cd /root/hermes-webui/hermes_data/.hermes/mywork
npm run build
```

**Step 4**：部署 dist 目录

```bash
wrangler pages deploy dist --project-name=quiz-helper
```

首次会提示创建项目，确认即可。后续更新只需重复 Step 3 + 4。

---

### 方式 C：直接上传 (Dashboard)

1. 本地执行 `npm run build`
2. 打开 Cloudflare Dashboard → Pages → 创建项目 → 直接上传
3. 把 `dist/` 文件夹拖入上传区
4. 项目名填 `quiz-helper`，点击部署

适合不需要 CI/CD 的场景。

---

## 4. 自定义域名

**添加域名**：Cloudflare Pages 项目 → 自定义域 → 设置

```
quiz.example.com
```

1. 域名 DNS 必须在 Cloudflare 管理（或 CNAME 指向 `项目名.pages.dev`）
2. 添加后自动签发 SSL 证书
3. 等待 DNS 生效（通常 1-5 分钟）

---

## 5. 环境变量

本项目不需要环境变量（纯前端，无 API Key），但如果将来需要：

Cloudflare Pages → 项目 → 设置 → 环境变量

| 变量名 | 说明 |
|--------|------|
| `VITE_APP_TITLE` | 应用标题（Vite 会在构建时注入） |

Vite 环境变量需要 `VITE_` 前缀才会暴露给前端代码，并在 `import.meta.env` 下访问。

---

## 6. 国内访问优化

### 6.1 域名选择

Cloudflare Pages 默认域名 `*.pages.dev` 在国内可能不稳定。强烈建议绑定**已在 Cloudflare 管理的自定义域名**。

### 6.2 IP 优选

如果自定义域名访问速度仍不理想，可以用 Cloudflare SAAS 回源 + 优选 IP：

1. 准备一个「回源域名」（托管在 Cloudflare 的任意域名）
2. Workers/Python 脚本轮询 Cloudflare IP 列表，选延迟最低的
3. 自选域名 CNAME 到优选 IP

> 不是必须的。对于纯静态小文件（~245KB gzipped），任何接入点都能在 1 秒内加载完毕。

---

## 7. 构建细节

### 7.1 构建环境

Cloudflare Pages 构建环境：

- OS: Ubuntu (x86_64)
- Node.js: 默认 LTS（可在环境变量中设置 `NODE_VERSION`）
- 内存: 足够的构建内存（Vite build 峰值约 200MB）

### 7.2 构建产物大小

| 文件 | 大小 | 说明 |
|------|------|------|
| `dist/index.html` | ~0.7 KB | 入口页面 |
| `dist/assets/index-XXXXXX.js` | 469 KB | Vue 3 + Router + xlsx + 业务逻辑 |
| `dist/assets/index-XXXXXX.css` | 23 KB | Tailwind CSS |
| `dist/assets/excel-worker-XXXXXX.js` | 335 KB | Web Worker (xlsx 解析逻辑) |
| `dist/_headers` | 65 B | 缓存策略 |

首次加载 ~245KB（gzip），后续页面切换 0 网络请求（纯 SPA + Hash 路由）。

### 7.3 构建日志排错

常见问题：

**Q: `vite: not found`** — 检查构建命令是否为 `npm run build`（不是直接 `vite build`）

**Q: `out of memory`** — Node 版本过旧或构建资源不足，设置环境变量 `NODE_OPTIONS: --max-old-space-size=512`

---

## 8. 本地预览（模拟 Cloudflare Pages）

Vite 自带的 preview 不能处理 `_redirects`。要本地模拟 Cloudflare Pages 行为：

```bash
# 安装 wrangler
npm install -g wrangler

# 在项目根目录运行
wrangler pages dev dist --port 8788
```

这会启动一个本地服务器，完全模拟 Cloudflare Pages 的路由和缓存策略。

---

## 9. 监控与分析

### 9.1 Cloudflare Web Analytics

免费、无需 Cookie 的流量分析：

1. Cloudflare Dashboard → 网站 → 你的域名 → Analytics
2. 支持 PV、UV、国家分布、带宽用量

### 9.2 Pages 专属分析

Cloudflare Pages → 项目 → 分析：

- 构建时长 & 成功率
- 带宽用量
- 请求数

---

## 10. 回滚与版本管理

### 10.1 回滚到历史部署

Cloudflare Pages → 项目 → 部署 → 选择历史部署 → 「回滚到此部署」

无需重新构建，秒级切换。

### 10.2 预览部署（Preview Deployments）

如果用 Git 方式部署，每个 Pull Request 自动生成预览 URL：

```
https://<commit-hash>.quiz-helper.pages.dev
```

方便在合并前测试变更。

---

## 11. 注意事项与限制

| 限制 | 说明 |
|------|------|
| 单个文件上限 | 25MB（本项目最大文件 469KB，完全不受影响）|
| 并发构建 | 免费版 1 次，Pro 版 5 次 |
| 单次构建时间 | 20 分钟（本项目构建 ~10 秒）|
| 每月构建次数 | 500 次（免费版）|
| `_redirects` 规则数 | 最多 100 条（本项目只用 1 条）|
| `_headers` 规则数 | 最多 100 条（本项目只用 1 条）|

---

## 12. 快速命令速查

```bash
# 本地开发
npm run dev                # 启动 Vite dev server → http://localhost:5173

# 构建
npm run build              # 输出到 dist/

# 本地预览 (Vite)
npm run preview            # → http://0.0.0.0:3002

# 本地预览 (wrangler，模拟 Cloudflare)
npx wrangler pages dev dist --port 8788

# 部署到 Cloudflare Pages (wrangler CLI)
npx wrangler pages deploy dist --project-name=quiz-helper
```
