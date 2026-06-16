# 饭堂订餐系统 - 部署说明

基于开源项目 V-BC（微报餐）改造。本目录提供 Docker 一键部署（MySQL + Redis + 后端）。

## 一、前置条件

- 一台**公网**云服务器（微信小程序要求后端是 **HTTPS 公网域名**）
- 服务器已装 **Docker** 和 **Docker Compose**
- 一个**已认证的微信小程序**（拿到 AppID、AppSecret）
- 一个解析到本服务器的**域名 + HTTPS 证书**（小程序后台要把该域名加入 request 合法域名）

## 二、部署步骤

```bash
# 1) 进入部署目录
cd server/deploy

# 2) 复制并填写配置（域名、数据库密码、微信 AppID/Secret 等）
cp .env.example .env
vi .env

# 3) 构建并启动（首次会在容器内用 JDK8 编译，约几分钟）
docker compose up -d --build

# 4) 查看后端日志，确认启动成功
docker compose logs -f app
```

看到 Spring Boot 的 `Started BcBootstrap` 即启动成功。

> 首次启动时 MySQL 会自动导入 `server/doc/数据.sql` 建表（仅当数据卷为空时执行一次）。

## 三、HTTPS / 反向代理

容器里后端监听 `8088`。生产环境请在前面加 **Nginx + HTTPS**，把你的域名反代到 `127.0.0.1:8088`，例如：

```nginx
server {
    listen 443 ssl;
    server_name canteen.your-company.com;
    ssl_certificate     /etc/nginx/cert/your.crt;
    ssl_certificate_key /etc/nginx/cert/your.key;
    location / {
        proxy_pass http://127.0.0.1:8088;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

然后在**微信公众平台 → 开发管理 → 开发设置 → 服务器域名**里，把 `https://canteen.your-company.com` 加入 **request 合法域名**。

## 四、小程序端配置

编辑 `wxapp/app.js`，把 `globalData.web_path` 改成你的后端域名、`appId` 改成你单位小程序的 AppID（详见根目录改造说明）。

## 五、初始化数据（首次）

系统是多租户结构，需要给你的小程序 AppID 建好基础数据（部门、报餐配置等）。
当前后台管理界面**待开发**；在此之前可临时用 SQL 或 Druid 监控台初始化。详见项目改造计划。

## 六、关掉了哪些原项目的云依赖

为单位内部自用，以下原项目依赖已**默认关闭/留空**，不影响报餐核心功能：

| 组件 | 状态 | 说明 |
|------|------|------|
| XXL-Job 定时任务 | 关闭（`XXL_JOB_RUNNABLE=off`）| 报餐用不上分布式调度 |
| 阿里云 OSS | 留空 | 仅 banner 图上传用到，无自动配置，不影响启动 |
| 阿里短信 | 留空 | 不影响启动 |
| 支付宝 / 微信支付 | 已移除 | 单位饭堂不收费 |

## 常用命令

```bash
docker compose ps              # 看状态
docker compose logs -f app     # 看后端日志
docker compose restart app     # 重启后端
docker compose down            # 停止（保留数据卷）
docker compose up -d --build   # 改了代码后重新构建并启动
```
