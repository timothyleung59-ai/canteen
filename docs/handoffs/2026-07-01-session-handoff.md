# 交接文档：饭堂报餐系统 (canteen)

> 生成时间：2026-07-01
> 仓库：https://github.com/timothyleung59-ai/canteen (fork 自开源项目 txlaijava/V-BC)
> 本机工作目录：`/Users/timothy/Claude/Canteen/V-BC`

## 项目背景

把 2019 年的开源"微报餐"小程序项目改造成单位内部饭堂报餐系统。核心决策（早期确定，未变过）：

- 报餐模式：只报人头，不选菜品
- 端：微信小程序（`wxapp/`）+ 新建的 Vue3 Web 管理后台（`admin-web/`）
- 支付：不需要
- 部署：Docker + 公网云服务器

详细的目录结构、后端接口契约、早期"评估后有意不改"的事项，都记录在仓库根目录的 **`HANDOFF.md`** 里，不在本文档重复，请直接读那份。

## 当前生产环境状态

- **服务器**：腾讯云 `124.222.32.27`（CentOS 7）。**注意这台机器不是独占的**，上面还跑着 park-*、hjyj-*、myagent-*、study-agent-* 等十几个其他项目，共享同一个宿主机 nginx（版本 1.20.1，比较老，不支持 `http2 on;` 新语法）。改动这台机器上的任何全局配置都要格外小心，只动 canteen 自己的部分。
- **Docker 容器**：`canteen-app`（8088，仅 `127.0.0.1`）、`canteen-mysql`、`canteen-redis`，用 `/root/canteen/docker-compose.prebuilt.yml` 管理。
- **域名**：`canteen.zsess.net`（已备案，DNS 走阿里云直连解析，**不是** Cloudflare 代理）。因为没有 CF 帮忙终结 HTTPS，是自己用 Let's Encrypt 签的真实证书，nginx 配置在服务器 `/etc/nginx/conf.d/canteen-zsess.conf`（仓库里的副本在 `server/deploy/nginx-canteen.zsess.net.conf`）。**证书自动续期已配置好 crontab**（`0 3 * * * certbot renew --quiet --deploy-hook "nginx -s reload"`），别删掉这条。
- **旧域名 `aidio.site` 已下线**：nginx 配置移到了 `/etc/nginx/conf.d.disabled/`（备份，未删除，可恢复）。但 **Cloudflare 上的 DNS 记录用户还没清理**，所以访问 aidio.site 不会报错，而是会显示这台机器上另一个项目（study-agent）的内容——这个清理动作只能用户自己去 CF 后台做，工具够不到。
- **Web 后台**：`https://canteen.zsess.net`，账号 `admin`，密码在服务器 `/root/canteen/.env` 的 `ADMIN_PASSWORD`（本文档不记录具体值，出于安全考虑）。
- **真实业务数据**：系统已经在被实际使用——32 名员工，1 个部门"研究中心"，有真实报餐记录，**改动/测试时要小心，别拿真实数据做危险操作**（本次会话多次通过插入`__测试__`前缀的临时记录来验证删除类功能，测完会清理掉，这是个可以延续的做法）。

## 部署方式（下次继续操作前必读）

1. **SSH 访问会话间不保留**：每次新会话都需要重新生成一对部署专用密钥（`ssh-keygen`），让用户在服务器控制台执行一条命令把公钥加到 `~/.ssh/authorized_keys`。这个流程用户已经很熟悉了，直接说"需要重新生成一把部署密钥，麻烦加一下"即可，不用长篇解释。
2. **后端构建**：本机 JDK 版本太高（17/26），不满足项目 Java 8 要求，必须用 Docker 容器构建：
   ```
   docker run --rm -v "$PWD":/app -v canteen-m2:/root/.m2 -w /app maven:3.9-eclipse-temurin-8 \
     mvn -s deploy/settings.xml -o -DskipTests -B clean package
   ```
   **⚠️ 大坑**：如果改了 `base/` 或 `framework/` 模块下的代码，Maven 会把旧的 RELEASE 版本 jar 缓存在 `.m2` 里复用，导致改动静默丢失（编译通过但线上行为没变）。这种情况必须先清缓存：
   ```
   rm -rf base/target server-bc/target framework/*/target parent/target
   rm -rf /root/.m2/repository/com/shopping   # 在构建容器内执行
   ```
   部署新 jar 前，务必解压验证改动真的进去了（`unzip -p ... | grep -ac 关键字`），不要只信"BUILD SUCCESS"。
3. **前端构建**：`cd admin-web && npm run build`，纯静态产物，`scp` 上传到服务器 `/var/www/canteen-admin/`（先清空旧的 `assets/` 目录再传，避免新旧文件混杂）。
4. **部署验证套路**：本地构建 → 上传（jar 先传成 `app.jar.new` 再 `mv` 覆盖，旧的备份成 `app.jar.bak.$(date +%H%M%S)`）→ `docker compose up -d --force-recreate app` → 等健康检查 200 → 用具体的业务请求验证（不要只看首页 200，那证明不了后端逻辑真的对）。前端验证可以用文件名 hash 比对本地 `dist/` 和线上 `curl` 返回的 `index.html` 引用，确认不是缓存的旧版本。

## 本次会话做的事（时间顺序，只列要点，细节看 commit）

`git log --oneline` 从 `d9ed560` 到最新 `7c90e3c`，大致是：

1. 小程序去短信验证（内部饭堂用不上）、去掉"统计" tab（改由有密码保护的 Web 后台承担）
2. 报餐设置保存改成按 appId upsert，修了一个会导致 500 的脏数据 bug
3. **中国法定节假日自动同步**：从开源库 `NateScarlet/holiday-cn` 拉取，每季度 1 次定时 + 启动时 + 手动触发三种时机，手动覆盖优先级最高，后端也做了强校验（不只是前端拦）
4. 预订页加"报本周/下周/本月/下月"和对应的批量取消快捷键
5. 域名迁移：`aidio.site`(Cloudflare) → `canteen.zsess.net`(阿里云直连 + 自签 Let's Encrypt 证书)，旧域名下线
6. 修了两个真实影响使用的小程序 bug：
   - Web 后台改报餐截止时间后，小程序报餐按钮永久禁用（`lunchOrderTime` 格式从"两段式区间"变成"单点时间"后，旧的 `split("-")` 解析逻辑失效，`calculateSeconds` 也不支持两段式时间格式，两个连带 bug 一起修的）
   - 取消报餐无论成功失败都提示"成功"，导致删了又"自己冒出来"（`CancellMeal` 没检查后端返回的业务 code）
7. **Web 后台移动端适配**（本次会话最后完成的工作）：左侧固定菜单窄屏下换成抽屉菜单；4 个列表页（员工/明细/统计/部门）表格窄屏下换成卡片列表（根因是这些表格都用了 `fixed="right"` 固定列，内容超宽时会把中间列直接遮挡到不可见）；报餐设置表单窄屏下 label 改纵向排列；登录页卡片宽度自适应。本地用 preview 工具在 375×812 和 1280×800 两种尺寸逐页验证过，已部署上线，并用构建文件 hash 比对确认线上确实是新版本（不是猜的，是真验证过的）。

## 尚未完成 / 下次会话要关注的事

1. **【最大风险点】小程序从未在真实微信环境跑过**：这么多轮改动（去短信验证、去统计 tab、报餐/预订/取消逻辑修复、批量报餐快捷键）全部只做过 `node --check` 静态语法检查，从没在微信开发者工具或真机上实际点过。下次会话如果用户提到"小程序有问题"，第一反应应该是"终于要测了"，引导用户在开发者工具里走一遍：注册（无验证码）→ 报餐/取消报餐 → 预订（含批量报本周/下周/本月/下月）→ 取消预订。
2. **小程序还没重新上传+提审+发布**：代码里 `wxapp/app.js` 的域名已经改成 `canteen.zsess.net` 了，但只有走完"开发者工具上传代码 → mp.weixin.qq.com 提交审核 → 审核通过后发布"，线上用户才会真正用上新域名和这些 bug 修复。**同时微信公众平台后台的"request 合法域名"需要从 `aidio.site` 换成 `canteen.zsess.net`**——这两步都只能用户自己在微信后台操作，工具做不到。
3. **"截止报餐时间后不能取消报餐"目前只在小程序前端生效**：服务端 `BcRecordServiceImpl.deleteBcRecordById` 没有对应的时间校验，理论上能绕过小程序直接调 API 在截止后删除。上次问过用户要不要在服务端也加固这条规则，还没得到回复。如果用户提起，可以参考节假日停餐的 `HolidayJudgeService` 强校验模式如法炮制。
4. **aidio.site 的 Cloudflare DNS 记录没清**：nginx 配置已下线，但 CF 那边还在代理，访问 aidio.site 会看到这台机器上别的项目内容，容易让人以为出 bug 了——提醒用户这是已知情况，需要他自己去 CF 后台删记录。
5. 服务器 `/root/canteen/` 下积累了不少次改动留下的 `.env.bak.*`、`app.jar.bak.*` 备份文件，找机会清一清旧的（非紧急）。

## 建议下一个 agent 先看这些

- 仓库根目录 **`HANDOFF.md`**：早期交接文档，目录结构、接口契约、"有意不改"的事项列表都在这
- `git log --oneline -30`：完整改动时间线，commit message 写得比较详细，很多"为什么这么改"的根因分析都在里面，不用我重复
- 如果要碰服务器：先说"需要重新生成部署密钥"，走标准流程

## 建议技能 (suggested skills)

- **run**：如果要本地起 admin-web 开发服务器验证/改动，用这个。本次会话已经配好了 `.claude/launch.json`（在 `/Users/timothy/Claude/Canteen/.claude/launch.json`，注意路径在 V-BC 上一层），里面临时写死了 `VITE_BACKEND` 指向生产环境方便登录测试，下次可按需调整回本地或调回默认。
- **verify**：用户报告"好像不对"的时候，先实际跑起来验证行为，别只看代码猜。本次会话吃过亏——最初把"截止时间后无法删除"当成 bug 分析了一通，后来才发现是用户故意设的产品规则，浪费了一轮。
- **security-review**：系统已上线且收集真实员工姓名/手机号，目前从没做过系统性安全审查（JWT 密钥强度、管理员密码强度、有没有未授权访问的接口、CORS 配置等），是个值得找机会做的事。
- **code-review**：这么多轮功能叠加和 bug 修复下来，代码库可能有值得整理的地方，可以考虑做一次系统性 review。

## 敏感信息处理说明

本文档没有写出：数据库密码、Web 后台管理员密码、微信小程序 AppSecret、JWT 签名密钥、任何 SSH 私钥内容。这些值都在服务器 `/root/canteen/.env`（需要先建立 SSH 访问才能读到），不应该也不需要出现在交接文档里。
