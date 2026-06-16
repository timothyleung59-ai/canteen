# 饭堂报餐系统 — 交接文档（HANDOFF）

> 最后更新：2026-06-17
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
| 管理端 | **新建一个 Web 后台**（已完成，见 `admin-web/`） |
| 部署 | 公网云服务器 + Docker，单位已有认证小程序 |

---

## 二、当前状态（本机已全链路验证）

三端主体都已跑通并验证，**剩下的基本是部署与填真实值，不是写代码**。

- ✅ **后端**：能构建、Docker 起 MySQL+Redis+后端跑通；配置全走环境变量；**本次会话修复 15+ 个 bug**（SQL / 业务逻辑 / 类型 / 安全，见第四节）
- ✅ **admin-web**（Vue3 后台）：依赖已装，Preview 浏览器**逐页实测通过**（登录/概览/明细/统计/员工/部门/设置），数据全对
- ✅ **小程序**：去品牌化为「饭堂报餐」；修了几个客户端 bug；**补全了原仓库缺失的全部图标**（`wxapp/images/`）
- ✅ 端到端验证：管理员登录→带 `Admin-Token` 调管理接口→真实数据；无/伪造 token→401；小程序登录态接口无 token→401

**剩下要做（多为你的基建/决策）**：见第九节「上线清单」。

---

## 三、目录结构

```
canteen/
├── server/                  后端 (Java 8 + SpringBoot 2.1.3, 多模块 Maven)
│   ├── parent/ base/ framework/   父POM / 领域实体仓储 / 9个 starter
│   ├── server-bc/           主应用（控制器/服务/过滤器/配置）
│   ├── doc/数据.sql          建表 SQL（容器首启自动导入）
│   ├── doc/seed-test.sql     ★ 本机验证测试数据（3部门/6员工/13记录/1配置，幂等）
│   ├── Dockerfile           多阶段构建 (JDK8 maven → JRE8，已设 TZ=Asia/Shanghai)
│   └── deploy/              docker-compose(.localtest).yml / .env.example / settings.xml / README.md
├── admin-web/               Vue3 Web 后台 (Vite + Element Plus)，依赖已装
├── wxapp/                   微信小程序（已去品牌化 + 已补图标）
│   └── images/              ★ 本次生成的图标资产（tabBar/页面/头像/授权图）
├── .claude/launch.json      Preview dev server 配置（起 admin-web 用）
└── HANDOFF.md               本文件
```

---

## 四、改动清单

### A. 基线改造（更早，让 2019 老项目能跑 + 加后台）
- 让多模块能构建（删失效私有仓库/依赖、移除引用不存在类的 starter、删失效 import）
- **修配置注入根因**：删掉 `BcBootstrap` 里过早实例化的自定义 `PropertySourcesPlaceholderConfigurer`（它导致 `${ENV:默认}` 永远读不到环境变量——历史「Access denied」真因）；新增 `YamlPropertySourceFactory` 让 `wx.yml` 仍能解析
- 新增 **Web 后台鉴权层**：`AdminProperties` / `AdminAuthController`(/admin/login,info,logout) / `AdminAuthFilter`(校验 `Admin-Token`) / `WebConfiguration` 注册
- 新增 **Vue3 后台 `admin-web/`** 与 **部署基建**（Dockerfile / compose / .env.example / settings.xml）

### B. 本次会话（2026-06-17）修复的 bug（均本机验证）
| 类别 | 修复 | 位置 |
|---|---|---|
| SQL | `getDepartmentPageList` / `countDinnerByDay` 的 `group by d.name`→`d.id`，修 MySQL `only_full_group_by` 直接 5000（生产 5.7 同样会炸） | BcUserDepartmentServiceImpl:48 / BcUserDepartmentRepository:19 |
| 前端 | Departments/Dashboard 把 `{data,total}` 当数组（取 `.data`）；Config 保存补传 `id`（否则每存 INSERT 新行致 `findByAppId` 多行异常） | admin-web/views/* |
| 逻辑 #4 | 注册激活状态反逻辑对调（免审核→已激活 / 需审核→待激活） | BcUserServiceImpl |
| 逻辑 #5 | 部门 total 改 `countByAppId`（原 `count()` 跨 appid 全表） | BcUserDepartmentController:73 |
| 逻辑 L1 | 报餐/预约服务端去重（同用户同日已报则拒） | BcRecordServiceImpl / BcReserveRecordServiceImpl |
| 逻辑 L3 | `@CurrentBcUser` 失效改抛 `BcUnauthorizedException` + 新增 `GlobalExceptionAdvice` 统一返 401（原 NPE 被吞成「服务器异常」） | resolver + GlobalExceptionAdvice |
| 逻辑 L4 | 取消报餐校验：已就餐 / 历史日期不可取消（越权防护保留） | BcRecordServiceImpl.deleteBcRecordById（返回类型改 ActionResult） |
| 逻辑 L6 | 报餐加周末闸门 + 午餐截止时间闸门（读 config 字段） | BcRecordServiceImpl.bcRecordMealSave |
| 安全 S1 | JWT 密钥改读 `JWT_SECRET` 环境变量（开发兜底+告警），`generalKey` 用 UTF-8 字节；消除「源码内置密钥可伪造 admin token」 | Constants / JwtTokenUtils + compose/.env |
| 安全 S2 | `AdminAuthFilter.PROTECTED` 补 `BcBanner/upload`、`BcBanner/deleteImg`、`bctch`；匹配由 `contains` 改**边界匹配**（修 `getBcRecordList` 误伤 `getBcRecordListByDinTime`=L2） | AdminAuthFilter |
| 小程序 | 去品牌化为「饭堂报餐」；报餐/预约防重提交；倒计时 `clearInterval`→`clearTimeout`；`error:`→`fail:`；移除未定义 `clickSwiper`；补全 16 个图标 | wxapp/* + wxapp/images/ |

### C. 评估后**有意未改**（非 bug 或风险大于收益）
- **L5 `bctch` 删除范围**：判定为「预约转报餐后清空当天预约」的清理逻辑，被删的是冗余预约，不丢餐信息——不动。
- **统计页分页 / 倒计时数学（`|| []`）**：`wxapp/pages/statistics/index.js`、`index.js countDown` 逻辑绕，本机无微信模拟器跑不了，盲改风险大——留待开发者工具里改。
- **类型「撒谎」（潜在）**：`countDinnerByDay` 声明返回实体实为数组（小程序统计页正按数组消费，能用）；`BcBannerServiceImpl` 声明 `List<BcBanner>` 实为 `List<Map>`（当前只序列化成 JSON，不崩）。
- **安全收尾项**（部署时处理）：admin 默认弱口令 `admin/admin123`、用户 token 永不过期、Druid 监控台弱口令+白名单——见第九节。

---

## 五、如何运行（本机）

### 后端构建（用 JDK8 容器，本机无需装 Java8）
```bash
cd server
# 首次全量构建：
docker run --rm -v "$PWD":/app -v canteen-m2:/root/.m2 -w /app \
  maven:3.9-eclipse-temurin-8 mvn -s deploy/settings.xml -DskipTests -B clean package
# 仅改了 server-bc 时的【增量】构建（快很多，避开 spring-boot repackage 幂等坑：先单独 clean server-bc）：
docker run --rm -v "$PWD":/app -v canteen-m2:/root/.m2 -w /app maven:3.9-eclipse-temurin-8 \
  sh -c "mvn -s deploy/settings.xml -o -pl server-bc clean && mvn -s deploy/settings.xml -o -DskipTests -pl server-bc -am package"
```

### 起 MySQL+Redis+后端（本机验证）
```bash
cd server/deploy && docker compose -f docker-compose.localtest.yml up -d
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8088/    # 200
```
- 本机管理员：`admin / admin123`，appid：`wxtest0000000000`，`JWT_SECRET=localtest-jwt-secret-0123456789abcdef`（见 localtest compose）。
- 测试数据：`docker cp server/doc/seed-test.sql canteen-mysql-test:/tmp/s.sql && docker exec canteen-mysql-test sh -c "mysql -uroot -ptestpass --default-character-set=utf8mb4 baocan -e 'source /tmp/s.sql'"`（幂等，可重复）。
- ⚠️ 改了 compose 的 env（如 JWT_SECRET）后要 `docker compose ... up -d` **重建**容器，`docker restart` 不会重读 env。

### admin-web（依赖已装）
```bash
cd admin-web && npm run dev    # http://localhost:5173，Vite 已代理 /bc /admin 到 :8088
```

### 生产部署（要点）
```bash
cd server/deploy && cp .env.example .env   # 填：域名、各密码、WX_APPID/SECRET、JWT_SECRET
docker compose up -d --build
```
- admin-web：`npm run build` → Nginx 托管 `dist/` + 反代 `/bc` `/admin` 到后端（同源，无需 CORS）。
- 小程序要求 HTTPS，Nginx 配 TLS（示例见 `deploy/README.md`）。

---

## 六、重要原理与坑（务必读）

1. **配置全走环境变量**：`DB_PASSWORD / WX_APPID / ADMIN_PASSWORD / JWT_SECRET` 等都从 env 注入，不要退回 `-D`。
2. **JWT 密钥**：生产**必须**在 `.env` 设强随机 `JWT_SECRET`（≥32 字符）；不设会回退到源码内置开发密钥并打 WARN，**任何拿到源码者都能伪造 admin token**。
3. **后台鉴权边界**：`AdminAuthFilter` 只拦 `PROTECTED` 里那些纯管理接口（员工/部门/明细/统计/导出/配置写/banner 上传删除/bctch），校验 `Admin-Token`。小程序与后台共用的只读接口（部门列表、读配置、今日人数、`getBcRecordListByDinTime`、**`confirmEat` 确认就餐**）保持开放——小程序用的是另一套 `@CurrentBcUser` 的 `Token` 头。
   - ⚠️ `confirmEat` **故意开放**：小程序统计页「确认就餐」按钮由普通用户调用，加进 PROTECTED 会 401 打断它。要收紧「任何用户都能核销」需单独产品决策。
   - 匹配用**边界匹配**（片段后须为串尾/`/`/`?`），别改回 `contains`（会让 `getBcRecordList` 误伤 `getBcRecordListByDinTime`）。
4. **原 `/bc/**` 整段在 `SecurityFilter` 是免登录白名单**——这是必须加 `AdminAuthFilter` 的原因；`@CurrentBcUser` 接口的鉴权全靠 resolver（已改为失效即抛 401）。
5. **构建用 JDK8**：本机是 JDK17/26，直接 `mvn` 会失败；务必用 `maven:3.9-eclipse-temurin-8` 容器。
6. **时区**：生产 Dockerfile 已设 `TZ=Asia/Shanghai`；localtest 的 app 容器走 UTC（仅测试无碍）。

---

## 七、小程序状态（已去品牌化，待填真实值）

- ✅ 文案/标题统一为「饭堂报餐」；`project.config.json` projectname/appid、各页「红商」字样已清。
- ✅ 图标已补：`wxapp/images/`（用 MDI 风格内联 SVG 本地渲染，普通灰/选中橙，透明背景，**可随时换成正式图标**）。
- ⏳ **必须填真实值**（我留了带 `TODO` 的占位）：
  - `wxapp/app.js`：`web_path`（后端 HTTPS 域名，不带结尾斜杠）、`appId`（你单位小程序 appid，**须与后端 `WX_APPID/ADMIN_APPID` 一致**）
  - `wxapp/project.config.json`：`appid`（微信平台 appid）
- ⏳ 小程序代码改动**本机没微信模拟器跑过**，需在开发者工具实跑验证（报餐/预约/取消/统计）。

---

## 八、后端管理接口契约

返回统一 `{ code, msg, data }`，`code=0` 成功。业务前缀 `/bc/{appid}/`，纯管理接口需请求头 `Admin-Token`（共用只读可不带）。

| 功能 | 方法 路径 | 关键参数 | data 结构 |
|---|---|---|---|
| 管理员登录 | POST `/admin/login` | username, password | `{token, appid, username, unitName}` |
| 员工分页 | GET `/BcUser/getUserPageList` | currentPage,pageSize,name,mobile,departmentId | `{data:[{id,name,mobile,department,status,user_department_id}], total}` |
| 改员工状态 | POST `/BcUser/updateStatusById` | id, isActive(bool) | int |
| 删除员工 | DELETE `/BcUser/delete` | id | — |
| 调整员工部门 | GET `/BcUser/editUserDepartmentId` | userDepartmentId, id | int |
| 导出员工 | POST `/BcUser/export` | name,mobile,departmentId | Excel(blob) |
| 部门列表(共用) | GET `/BcUserDepartment/getBcUserDepartmentList` | — | `[{id,appId,name}]` |
| 部门+人数 | GET `/BcUserDepartment/getDepartmentPageList` | currentPage,pageSize | `{data:[{name,headcount,id}], total}`（total 已改为按 appid 计数） |
| 部门增/改名/删 | POST `/save`、POST `/updateName`、DELETE `/deleteById/{id}` | — | — |
| 各部门今日人数(共用) | GET `/BcUserDepartment/countDinnerByDay` | curIndex | `[[count,name,id],...]`（数组套数组） |
| 报餐明细 | GET `/BcRecord/getBcRecordList` | currentPage,pageSize,name,mobile,departmentId,startTime,endTime | `{data:[{dinTime,bcType,bcChannel,name,mobile,deptName}], total:[{count}]}` |
| 报餐统计 | GET `/BcRecord/countBcRecordPageList` | 同上 | `{data:[{deptName,name,mobile,num}], total:int}` |
| 今日/明日人数(共用) | GET `/BcRecord/getTotalRecordByDinTime` | curIndex(0今/1明/-1昨) | int |
| 就餐名单(共用) | GET `/BcRecord/getBcRecordListByDinTime` | curIndex,currentPage,pageSize,deptId | list（需有 bc_user_wx 关联才出行） |
| 确认就餐(共用) | GET `/BcRecord/confirmEat` | id | int |
| 导出明细/统计 | POST `/BcRecord/export`、`/BcRecord/exportCount` | 同查询参数 | Excel(blob) |
| 读/存配置 | GET `/config/getConfig`(共用) / POST `/config/saveOrUpdate` | userNeedApprove,lunchCanMeal,dinnerCanMeal,lunchOrderTime,dinnerOrderTime,saturdayCanDiner,sundayCanDiner,id(存时带) | — |

---

## 九、上线清单（剩余，多为基建/决策）

1. **后端部署**：`server/deploy/` 下 `cp .env.example .env`，填真实域名 + **改默认 admin/DB/Redis/Druid 密码** + `WX_APPID/SECRET` + 强 `JWT_SECRET`，`docker compose up -d --build`。
2. **admin-web 上线**：`npm run build` → Nginx 托管 `dist/`，反代 `/bc` `/admin` 到后端，配 HTTPS。
3. **小程序填真值**：`app.js` 的 `web_path`+`appId`、`project.config.json` 的 `appid`（第七节）；微信公众平台「开发设置」把后端域名加进 request 合法域名；上传体验版。
4. **小程序实跑验证**：开发者工具点测报餐/预约/取消/统计（客户端改动本机未跑）。
5. **安全收尾**（建议）：上线后跟进 admin 登录限流、用户 token 设过期、Druid 监控台网关层屏蔽。
6. **可选打磨**：换正式图标；在开发者工具里修统计页分页 / 倒计时逻辑（第四节 C）。
