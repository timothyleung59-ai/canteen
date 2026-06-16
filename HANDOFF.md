# 饭堂报餐系统 — 交接文档（HANDOFF）

> 最后更新：2026-06-16
> 基线项目：[txlaijava/V-BC](https://github.com/txlaijava/V-BC)（2019 年的「微报餐」开源项目，SpringBoot 2.1.3 + 微信小程序）
> 本仓库在其基础上改造为**单位内部饭堂报餐系统**。

---

## 一、目标与已确定的方案

把开源的 V-BC 改造成本单位饭堂订餐系统，已确认的需求：

| 项目 | 决定 |
|---|---|
| 模式 | **只报人头**（不选菜品 / 不点菜） |
| 端 | 继续用**微信小程序** |
| 支付 | **不需要**（免费 / 工资扣款，线下处理） |
| 管理端 | **新建一个 Web 后台**（本次已完成主体，见下文 `admin-web/`） |
| 部署 | 公网云服务器 + Docker，单位已有认证小程序 |

---

## 二、当前状态（本地已验证通过）

后端 + Web 后台主体已经跑通，本机用 Docker 全链路验证 OK：

- ✅ 后端可构建（修复了 5 处历史遗留的依赖/代码问题），产出 `server-bc-1.0.0.RELEASE.jar`
- ✅ Docker 一键起 MySQL + Redis + 后端，**全部配置走环境变量**（占位符解析 bug 已修复，见第六节）
- ✅ 新增**管理员登录 + 鉴权层**：纯管理接口需登录态，小程序共用的只读接口保持开放
- ✅ 新增 **Vue3 Web 后台**（`admin-web/`），覆盖：登录、概览、报餐明细、报餐统计、员工管理、部门管理、报餐设置
- ✅ 端到端验证：登录 → 带 Token 调管理接口 → 返回真实数据；无 Token → 401

**尚未做**（下一步）：
- ⏳ 前端 `npm install` + 浏览器实际点测（依赖未装；代码已写完，接口契约已逐个 curl 验证）
- ⏳ 小程序去品牌化（当前仍是「红商集团」字样和 appid，见第七节）
- ⏳ 正式部署到公网服务器
- ⏳ 安全收尾：改默认管理员密码、填真实 appid/secret

---

## 三、目录结构

```
V-BC/
├── server/                  后端 (Java 8 + SpringBoot 2.1.3, 多模块 Maven)
│   ├── parent/              父 POM
│   ├── base/                领域实体 / 仓储 / 工具
│   ├── framework/           9 个 starter (druid/redis/token/...)
│   ├── server-bc/           主应用（控制器/服务/过滤器/配置）
│   ├── doc/数据.sql          建表 SQL（容器首启自动导入）
│   ├── Dockerfile           多阶段构建 (JDK8 maven → JRE8)
│   └── deploy/
│       ├── docker-compose.yml            ★ 生产部署（env 驱动）
│       ├── docker-compose.localtest.yml  本机验证用
│       ├── .env.example                  配置模板（复制为 .env）
│       ├── settings.xml                  Maven 阿里云镜像
│       └── README.md                     部署说明（含 Nginx HTTPS 示例）
├── admin-web/               ★ 新增：Vue3 Web 后台 (Vite + Element Plus)
├── wxapp/                   微信小程序（待去品牌化）
└── HANDOFF.md               本文件
```

---

## 四、本次改动清单

### 后端 — 让 2019 老项目能构建（5 处）
1. `server/pom.xml`、`server/parent/pom.xml`：删除两个已失效的私有 Maven 仓库
2. `server/server-bc/pom.xml`：删除两个用不到且拉不到的依赖（`k12-alipay-sdk`、`spring-boot-starter-pay`）
3. `server/framework/pom.xml`：从 `<modules>` 移除 `spring-boot-starter-token`（引用了不存在的类）
4. `server/base/.../BcUserWxRepository.java`：删除两个失效 import

### 后端 — 修复配置注入根因（关键）
- `server-bc/.../BcBootstrap.java`：**删除自定义的 `PropertySourcesPlaceholderConfigurer`**。它作为 BeanFactoryPostProcessor 实例化过早，拿不到 `Environment`，导致 `${ENV:默认值}` 占位符**永远读不到环境变量**（DB 密码、WX appid 等全部静默回退到默认值，这是之前一直「Access denied」的真正原因）。
- 新增 `server-bc/.../wx/conf/YamlPropertySourceFactory.java`，并把 `@PropertySource(value="classpath:wx.yml", factory=YamlPropertySourceFactory.class)` 放到 `WxMaConfiguration` 上 —— 保证 `wx.yml` 仍能被解析（`@PropertySource` 默认不支持 YAML），同时占位符改由 Spring Boot 默认解析器解析，**环境变量注入恢复正常**。

### 后端 — 新增 Web 后台鉴权层（4 文件 + 2 改）
- `server-bc/.../wx/admin/AdminProperties.java`：管理员账号配置（`admin.*`）
- `server-bc/.../wx/admin/AdminAuthController.java`：`/admin/login`、`/admin/info`、`/admin/logout`，登录成功签发 JWT（复用现有 `JwtTokenUtils`）
- `server-bc/.../wx/filter/AdminAuthFilter.java`：只拦截**纯管理接口**，校验请求头 `Admin-Token`
- `server-bc/.../WebConfiguration.java`：注册 `AdminAuthFilter`（order=0，先于原 `SecurityFilter`）
- `server-bc/.../wx/filter/SecurityFilter.java`：白名单加 `/admin`
- `server-bc/.../config/application-pro.properties`：新增 `admin.*` 配置（env 驱动）

### 新增 — Vue3 Web 后台 `admin-web/`
- 技术栈：Vue 3 + Vite + Element Plus + axios + vue-router + pinia
- `src/api/request.js`：axios 实例，自动带 `Admin-Token`，统一解包 `{code,msg,data}`，401 自动跳登录
- `src/api/bc.js`：所有后端接口封装
- `src/views/*`：Login / Dashboard / Records / Stats / Employees / Departments / Config

### 部署基建（新增）
- `Dockerfile`、`deploy/docker-compose.yml`、`deploy/docker-compose.localtest.yml`、`deploy/.env.example`、`deploy/settings.xml`、`deploy/README.md`

---

## 五、如何运行

### 后端（本机验证）
```bash
cd server
# 用 JDK8 容器构建（本机无需装 Java8）
docker run --rm -v "$PWD":/app -v canteen-m2:/root/.m2 -w /app \
  maven:3.9-eclipse-temurin-8 mvn -s deploy/settings.xml -DskipTests -B clean package
# 起 MySQL + Redis + 后端
cd deploy && docker compose -f docker-compose.localtest.yml up -d
# 健康检查
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8088/    # 200
```
本机管理员账号：`admin / admin123`，appid：`wxtest0000000000`（见 localtest compose）。
测试数据可用 `server/doc/数据.sql` 之外手工 seed（3 部门 / 6 员工 / 若干报餐记录）。

### 前端（Web 后台）
```bash
cd admin-web
npm install
npm run dev        # http://localhost:5173 ，Vite 已配置把 /bc 和 /admin 代理到 localhost:8088
```
> 前端依赖尚未安装过，这是下一步要做的第一件事。

### 生产部署（要点）
```bash
cd server/deploy
cp .env.example .env      # 填：域名、DB/Redis/Druid/Admin 密码、WX_APPID、WX_SECRET
docker compose up -d --build
```
- 前端：`cd admin-web && npm run build`，把 `dist/` 交给 Nginx 托管，并把 `/bc` `/admin` 反代到后端（同源，无需 CORS）。
- 小程序要求 HTTPS，Nginx 配 TLS（示例见 `deploy/README.md`）。

---

## 六、重要原理与坑（务必读）

1. **配置注入只认环境变量了**：根因已修复，`docker-compose.yml` 里的 `DB_PASSWORD / WX_APPID / ADMIN_PASSWORD` 等环境变量现在能正确注入。**不要**再退回去用 `-D` 那套（localtest 早期版本用过，现已统一成 env）。

2. **后台鉴权的边界**：`AdminAuthFilter` 只拦截「小程序不会调用」的纯管理接口（员工分页/改状态/删除/导出/改部门、部门增删改分页、报餐明细/统计/导出、改配置）。小程序与后台**共用的只读接口**（部门列表、读配置、当天人数、确认就餐等）保持开放——因为小程序登录用的是另一套 `@CurrentBcUser` Token，不能强加 `Admin-Token`。详见 `AdminAuthFilter.PROTECTED` 列表。

3. **`bc` 整个前缀在原 `SecurityFilter` 里是免登录白名单**（`GreenUrlSet.add("/bc")`）。也就是说原项目的所有 `/bc/**` 管理接口本来**毫无鉴权**——这正是必须加 `AdminAuthFilter` 的原因。

4. **原项目一个疑似 bug（未改动，注意）**：`BcUserServiceImpl.bcUserRegister` 里，`userNeedApprove=false` 时却把新用户置为「未激活(0)」、`=true` 时置为「激活(1)」，与字面直觉相反。当前未动它以免影响小程序流程，排查报餐权限问题时留意。

5. **`getDepartmentPageList` 的 total 统计**用了 `repository.count()`（跨 appid 全表计数），单单位场景无影响，但多 appid 会偏大。后台部门页用的是返回的 list（含每部门人数 `headcount`），不依赖这个 total。

6. **构建用 JDK8**：本机是 JDK17/26，直接 `mvn` 会失败；务必用 `maven:3.9-eclipse-temurin-8` 容器构建（Dockerfile 已是如此）。

---

## 七、小程序去品牌化（待做）

`wxapp/` 里仍有「红商集团」字样和原 appid，部署前需替换：
- `wxapp/app.js`：`web_path`（改成你单位后端域名）、`appId`（改成你单位小程序 appid `wx...`）
- `wxapp/app.json`：`navigationBarTitleText`（"红商集团报餐" → 你单位名）
- `wxapp/pages/index/index.js`：硬编码的「红商家人」「红商集团报餐小程序」等文案

---

## 八、后端管理接口契约（后台前端对接用）

返回统一为 `{ code, msg, data }`，`code=0` 成功。业务接口前缀 `/bc/{appid}/`，需在请求头带 `Admin-Token`（共用只读接口可不带）。

| 功能 | 方法 路径 | 关键参数 | data 结构 |
|---|---|---|---|
| 管理员登录 | POST `/admin/login` | username, password | `{token, appid, username, unitName}` |
| 员工分页 | GET `/BcUser/getUserPageList` | currentPage,pageSize,name,mobile,departmentId | `{data:[{id,name,mobile,department,status,user_department_id}], total}` |
| 改员工状态 | POST `/BcUser/updateStatusById` | id, isActive(bool) | int |
| 删除员工 | DELETE `/BcUser/delete` | id | — |
| 调整员工部门 | GET `/BcUser/editUserDepartmentId` | userDepartmentId, id | int |
| 导出员工 | POST `/BcUser/export` | name,mobile,departmentId | Excel(blob) |
| 部门列表(共用) | GET `/BcUserDepartment/getBcUserDepartmentList` | — | `[{id,appId,name}]` |
| 部门+人数 | GET `/BcUserDepartment/getDepartmentPageList` | currentPage,pageSize | `[{name,headcount,id}]` |
| 新增部门 | POST `/BcUserDepartment/save` | name | — |
| 改部门名 | POST `/BcUserDepartment/updateName` | id, name | int |
| 删部门 | DELETE `/BcUserDepartment/deleteById/{id}` | — | int |
| 报餐明细 | GET `/BcRecord/getBcRecordList` | currentPage,pageSize,name,mobile,departmentId,startTime,endTime | `{data:[{dinTime,bcType,bcChannel,name,mobile,deptName}], total:[{count}]}` |
| 报餐统计 | GET `/BcRecord/countBcRecordPageList` | 同上 | `{data:[{deptName,name,mobile,num}], total:int}` |
| 今日/明日人数(共用) | GET `/BcRecord/getTotalRecordByDinTime` | curIndex(0今/1明/-1昨) | int |
| 导出明细/统计 | POST `/BcRecord/export`、`/BcRecord/exportCount` | 同查询参数 | Excel(blob) |
| 读配置(共用) | GET `/config/getConfig` | — | `{userNeedApprove,lunchCanMeal,dinnerCanMeal,lunchOrderTime,dinnerOrderTime,saturdayCanDiner,sundayCanDiner,...}` |
| 存配置 | POST `/config/saveOrUpdate` | 上述各字段 | — |

---

## 九、下一步建议顺序

1. `cd admin-web && npm install && npm run dev`，浏览器实点各页面，修 UI 细节。
2. 小程序去品牌化（第七节），填后端域名 + 小程序 appid/secret。
3. 服务器：填 `.env`（**改默认 admin/DB/Redis 密码**），`docker compose up -d --build`，Nginx 配 HTTPS + 托管 `admin-web/dist`。
4. 小程序后台「开发设置」里把后端域名加进 request 合法域名，上传体验版联调。
