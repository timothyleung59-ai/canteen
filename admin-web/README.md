# 饭堂报餐 — Web 管理后台

Vue 3 + Vite + Element Plus，配套 `server/` 后端的管理界面。

## 开发
```bash
npm install
npm run dev      # http://localhost:5173
```
Vite 已把 `/bc`、`/admin` 代理到 `http://localhost:8088`（后端）。
若后端不在本机，改 `vite.config.js` 里的 `BACKEND` 或设环境变量 `VITE_BACKEND`。

默认本机后端管理员账号：`admin / admin123`。

## 构建部署
```bash
npm run build    # 产物在 dist/
```
把 `dist/` 交给 Nginx 托管，并将 `/bc`、`/admin` 反向代理到后端（同源，无需 CORS）。

## 页面
- 概览 Dashboard — 今日/明日报餐、员工数、部门人数
- 报餐明细 / 报餐统计 — 查询、分页、导出 Excel
- 员工管理 — 审核激活、调部门、删除、导出
- 部门管理 — 增删改
- 报餐设置 — 是否审核、午/晚餐开关与截止时间、周末是否可报

接口契约见仓库根目录 `HANDOFF.md` 第八节。
