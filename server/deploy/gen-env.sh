#!/usr/bin/env bash
# 在服务器上生成 canteen 的 .env（密码现场随机生成）。幂等：已存在则不重生成。
set -e
cd "$(dirname "$0")"
if [ -f .env ]; then
  echo ".env 已存在，跳过生成（避免改密码导致已初始化的 MySQL 连不上）"
  grep -E 'ADMIN_USERNAME|ADMIN_PASSWORD' .env
  exit 0
fi
DB=$(openssl rand -hex 16)
RD=$(openssl rand -hex 16)
DR=$(openssl rand -hex 12)
JW=$(openssl rand -hex 32)
AP=$(openssl rand -hex 9)
cat > .env <<EOF
DB_PASSWORD=$DB
REDIS_PASSWORD=$RD
DRUID_USER=druidadmin
DRUID_PASSWORD=$DR
ADMIN_USERNAME=admin
ADMIN_PASSWORD=$AP
ADMIN_UNIT_NAME=饭堂报餐管理后台
JWT_SECRET=$JW
WX_APPID=wxtest0000000000
WX_SECRET=placeholder_wx_secret
BASE_PATH=http://127.0.0.1:8088/
APP_PATH=http://127.0.0.1:8088/
EOF
chmod 600 .env
echo "===== 管理员登录凭据（请记录）====="
echo "  用户名: admin"
echo "  密码:   $AP"
echo "===== 其余密码已写入 .env (chmod 600)；WX_APPID 暂为占位 ====="
