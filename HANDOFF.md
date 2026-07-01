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

## 二、当前状态

### ✅ 后端已在服务器上运行

- 服务器 `124.222.32.27`，3 个容器（mysql / redis / app）通过 `docker-compose.prebuilt.yml` 运行
- 后端绑定 `127.0.0.1:8088`（仅本机访问，对外需 Nginx 反代）
- 管理员登录已验证通过
- 内存用量正常：app 292M/768M, mysql 74M/600M, redis 2M/96M

### ✅ 本机已全链路验证

- 后端能构建、Docker 起 MySQL+Redis+后端跑通；配置全走环境变量；**修复 15+ 个 bug**
- admin-web（Vue3 后台）逐页实测通过
- 小程序去品牌化为「饭堂报餐」；补全了原仓库缺失的全部图标

### ✅ 域名 / HTTPS（2026-07-01 更新）

- **当前主力域名：`canteen.zsess.net`**（已备案，DNS 走阿里云直连解析，不经过 Cloudflare）
  - 源站自己用 Let's Encrypt 签发真实证书（因为没有 CF 代理帮忙终结 HTTPS）
  - 证书自动续期：crontab `0 3 * * * certbot renew --quiet --deploy-hook "nginx -s reload"`（这台机器之前完全没有续期机制，部署时新加的，证书 90 天到期，千万别删这条 cron）
  - nginx 配置模板：`server/deploy/nginx-canteen.zsess.net.conf`
  - 后端 `.env` 的 `BASE_PATH`/`APP_PATH` 已指向这个域名
  - 小程序 `wxapp/app.js` 的 `web_path` 已指向这个域名（**改完还需在 mp.weixin.qq.com 重新上传+提审+发布**才对线上用户生效；微信后台的 request 合法域名也要同步换成这个）
- 旧域名 `aidio.site` **已于 2026-07-01 下线**：nginx 配置从 `/etc/nginx/conf.d/` 移到了 `/etc/nginx/conf.d.disabled/canteen-aidio.conf.bak`（未删除，可随时恢复）。仓库里的 `server/deploy/nginx-aidio.site.conf` 仅作历史参考。
  - ⚠️ 直接访问 `aidio.site` 现在不会 404，而是落到这台共享服务器的默认 server（另一个项目 study-agent），因为 Cloudflare 上的 DNS 记录还在代理——**要彻底下线需要去 Cloudflare 后台删掉/暂停这个域名的代理**，这一步只能人工做

### 待做

1. 微信公众平台 request 合法域名换成 `canteen.zsess.net`，小程序重新上传+提审+发布
2. 填入真实 WX_APPID / WX_SECRET（如果还没填）
3. 导入种子数据到生产 MySQL（如果还没导）
4. 小程序在开发者工具/真机里完整跑一遍（报餐/预订/取消/节假日停餐等，目前只做过静态语法检查）

---

## 三、目录结构

```
canteen/
├── server/                  后端 (Java 8 + SpringBoot 2.1.3, 多模块 Maven)
│   ├── parent/ base/ framework/   父POM / 领域实体仓储 / 6个 starter（已清理掉 mq/DynamicDruid/token 等死模块）
│   ├── server-bc/           主应用（控制器/服务/过滤器/配置）
│   ├── doc/数据.sql          建表 SQL（容器首启自动导入）
│   ├── doc/seed-test.sql     ★ 本机验证测试数据（3部门/6员工/13记录/1配置，幂等）
│   ├── Dockerfile           多阶段构建 (JDK8 maven → JRE8，已设 TZ=Asia/Shanghai)
│   └── deploy/
│       ├── docker-compose.yml               完整构建部署
│       ├── docker-compose.localtest.yml     本机测试
│       ├── docker-compose.prebuilt.yml      ★ 服务器轻量部署（预构建 jar，有内存限制）
│       ├── gen-env.sh                       ★ 服务器 .env 生成脚本（随机密码）
│       ├── .env.example / settings.xml / README.md
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

### B. Bug 修复（2026-06-17，均验证通过）
| 类别 | 修复 | 位置 |
|---|---|---|
| SQL | `getDepartmentPageList` / `countDinnerByDay` 的 `group by d.name`→`d.id`，修 MySQL `only_full_group_by` 直接 5000 | BcUserDepartmentServiceImpl:48 / BcUserDepartmentRepository:19 |
| 前端 | Departments/Dashboard 把 `{data,total}` 当数组（取 `.data`）；Config 保存补传 `id` | admin-web/views/* |
| 逻辑 #4 | 注册激活状态反逻辑对调（免审核→已激活 / 需审核→待激活） | BcUserServiceImpl |
| 逻辑 #5 | 部门 total 改 `countByAppId`（原 `count()` 跨 appid 全表） | BcUserDepartmentController:73 |
| 逻辑 L1 | 报餐/预约服务端去重（同用户同日已报则拒） | BcRecordServiceImpl / BcReserveRecordServiceImpl |
| 逻辑 L3 | `@CurrentBcUser` 失效改抛 `BcUnauthorizedException` + 新增 `GlobalExceptionAdvice` 统一返 401 | resolver + GlobalExceptionAdvice |
| 逻辑 L4 | 取消报餐校验：已就餐 / 历史日期不可取消 | BcRecordServiceImpl.deleteBcRecordById |
| 逻辑 L6 | 报餐加周末闸门 + 午餐截止时间闸门 | BcRecordServiceImpl.bcRecordMealSave |
| 安全 S1 | JWT 密钥改读 `JWT_SECRET` 环境变量 | Constants / JwtTokenUtils + compose/.env |
| 安全 S2 | `AdminAuthFilter.PROTECTED` 补全 + 边界匹配（修误伤） | AdminAuthFilter |
| 部署 | `DRUID_PASSWORD`→`DRUID_MONITOR_PASSWORD`（Spring Boot relaxed binding 会把 `DRUID_PASSWORD` 映射到 `druid.password` 覆盖数据库密码）；去掉 Druid config filter（会干扰环境变量密码注入） | docker-compose*.yml / application-pro.properties |
| 小程序 | 去品牌化；防重提交；clearInterval→clearTimeout；error:→fail:；移除未定义 handler；补全 16 个图标 | wxapp/* |

### C. 评估后**有意未改**
- **L5 `bctch` 删除范围**：判定为「预约转报餐后清空当天预约」的清理逻辑，被删的是冗余预约——不动。
- **统计页分页 / 倒计时**：无微信模拟器跑不了，盲改风险大——留待开发者工具里改。
- **类型「撒谎」**：`countDinnerByDay` 声明返回实体实为数组；`BcBannerServiceImpl` 声明 `List<BcBanner>` 实为 `List<Map>`——当前能用，不动。
- **安全收尾项**：admin 登录限流、用户 token 设过期、Druid 监控台网关层屏蔽——部署时处理。

---

## 五、如何运行（本机）

### 后端构建（用 JDK8 容器，本机无需装 Java8）
```bash
cd server
# 首次全量构建：
docker run --rm -v "$PWD":/app -v canteen-m2:/root/.m2 -w /app \
  maven:3.9-eclipse-temurin-8 mvn -s deploy/settings.xml -DskipTests -B clean package
# 仅改了 server-bc 时的增量构建：
docker run --rm -v "$PWD":/app -v canteen-m2:/root/.m2 -w /app maven:3.9-eclipse-temurin-8 \
  sh -c "mvn -s deploy/settings.xml -o -pl server-bc clean && mvn -s deploy/settings.xml -o -DskipTests -pl server-bc -am package"
```

### 起 MySQL+Redis+后端（本机验证）
```bash
cd server/deploy && docker compose -f docker-compose.localtest.yml up -d
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8088/    # 200
```

### admin-web（依赖已装）
```bash
cd admin-web && npm run dev    # http://localhost:5173
```

---

## 六、服务器部署（已完成后端）

### 已部署
```bash
# 服务器 124.222.32.27 上 /root/canteen/ 目录：
# app.jar / schema.sql / seed-test.sql / docker-compose.prebuilt.yml / gen-env.sh / .env

# 容器状态：
# canteen-app    292M/768M   127.0.0.1:8088
# canteen-mysql   74M/600M   内部 3306
# canteen-redis    2M/96M    内部 6379

# 管理：
docker compose -f docker-compose.prebuilt.yml logs -f app     # 看日志
docker compose -f docker-compose.prebuilt.yml restart app     # 重启
docker compose -f docker-compose.prebuilt.yml down             # 停止
```

### 待部署
1. **Nginx 反代**：`127.0.0.1:8088` → 公网域名，配 HTTPS
2. **admin-web**：`npm run build` → Nginx 托管 `dist/`，反代 `/bc` `/admin` 到后端
3. **种子数据**：`docker exec -i canteen-mysql mysql -uroot -p<密码> baocan < seed-test.sql`
4. **小程序**：填真实 appid + 后端域名，上传体验版

---

## 七、重要原理与坑（务必读）

1. **配置全走环境变量**：`DB_PASSWORD / WX_APPID / ADMIN_PASSWORD / JWT_SECRET` 等都从 env 注入。
2. **JWT 密钥**：生产**必须**在 `.env` 设强随机 `JWT_SECRET`（≥32 字符）；不设会回退到源码内置开发密钥。
3. **DRUID_PASSWORD 命名陷阱**：Spring Boot relaxed binding 会把 `DRUID_PASSWORD` 映射为 `druid.password`（数据库连接密码），覆盖真正的 DB 密码。已改名为 `DRUID_MONITOR_PASSWORD` / `DRUID_MONITOR_USER`。同理，任何以 `DRUID_` 开头的环境变量都可能映射到 `druid.*` 属性——加新环境变量时注意。
4. **Druid config filter**：老版本 Druid (1.0.28) 的 `config` filter 会干扰密码传递，已从 filters 列表中移除，改为环境变量可覆盖：`DRUID_FILTERS=stat,wall,log4j`。
5. **后台鉴权边界**：`AdminAuthFilter` 只拦 `PROTECTED` 列表里的纯管理接口。`confirmEat` **故意开放**——小程序统计页普通用户要用。匹配用边界匹配，别改回 `contains`。
6. **构建用 JDK8**：本机是高版本 JDK，务必用 `maven:3.9-eclipse-temurin-8` 容器。

---

## 八、后端管理接口契约

返回统一 `{ code, msg, data }`，`code=0` 成功。纯管理接口需请求头 `Admin-Token`。

| 功能 | 方法 路径 | 关键参数 | data 结构 |
|---|---|---|---|
| 管理员登录 | POST `/admin/login` | username, password (form) | `{token, appid, username, unitName}` |
| 员工分页 | GET `/BcUser/getUserPageList` | currentPage,pageSize,name,mobile,departmentId | `{data:[...], total}` |
| 改员工状态 | POST `/BcUser/updateStatusById` | id, isActive(bool) | int |
| 删除员工 | DELETE `/BcUser/delete` | id | — |
| 调整员工部门 | GET `/BcUser/editUserDepartmentId` | userDepartmentId, id | int |
| 导出员工 | POST `/BcUser/export` | name,mobile,departmentId | Excel(blob) |
| 部门列表(共用) | GET `/BcUserDepartment/getBcUserDepartmentList` | — | `[{id,appId,name}]` |
| 部门+人数 | GET `/BcUserDepartment/getDepartmentPageList` | currentPage,pageSize | `{data:[...], total}` |
| 部门增/改名/删 | POST `/save`、POST `/updateName`、DELETE `/deleteById/{id}` | — | — |
| 报餐明细 | GET `/BcRecord/getBcRecordList` | currentPage,pageSize,name,mobile,departmentId,startTime,endTime | `{data:[...], total:[{count}]}` |
| 报餐统计 | GET `/BcRecord/countBcRecordPageList` | 同上 | `{data:[...], total:int}` |
| 确认就餐(共用) | GET `/BcRecord/confirmEat` | id | int |
| 读/存配置 | GET `/config/getConfig` / POST `/config/saveOrUpdate` | 各字段 | — |
