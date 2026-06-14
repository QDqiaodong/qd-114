#!/bin/bash

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

echo "============================================"
echo "  花卉种子培育跟踪系统 - 启动中"
echo "============================================"
echo ""

if [ -f .env ]; then
    set -a
    source .env
    set +a
else
    echo "警告: .env 文件不存在，使用默认配置"
    FRONTEND_PORT=3008
    BACKEND_PORT=8088
    MYSQL_PORT=3309
    REDIS_PORT=6380
fi

check_port_occupied() {
    local port=$1
    lsof -nP -iTCP:"$port" -sTCP:LISTEN >/dev/null 2>&1
}

resolve_port() {
    local var_name=$1
    local label=$2
    local start_port=${!var_name}
    local port=$start_port
    local max_port=$((start_port + 200))

    while check_port_occupied "$port"; do
        echo "⚠️  $label 端口 $port 已占用，尝试 $((port + 1))"
        port=$((port + 1))
        if [ "$port" -gt "$max_port" ]; then
            echo "错误: $label 从 $start_port 起连续 200 个端口均不可用"
            exit 1
        fi
    done

    export "$var_name=$port"
    if [ "$port" != "$start_port" ]; then
        echo "✅ $label 已自动顺延为 $port"
    else
        echo "✅ $label 端口 $port 可用"
    fi
}

echo "正在检查端口（占用时自动顺延）..."
resolve_port FRONTEND_PORT "前端"
resolve_port BACKEND_PORT "后端"
resolve_port MYSQL_PORT "MySQL"
resolve_port REDIS_PORT "Redis"
echo ""

echo "正在启动 Docker 容器..."
echo ""

docker compose --env-file "$SCRIPT_DIR/.env" up -d --build

echo ""
echo "正在等待服务就绪..."

MAX_RETRIES=30
RETRY=0

while [ $RETRY -lt $MAX_RETRIES ]; do
    if curl -s "http://127.0.0.1:${FRONTEND_PORT}" > /dev/null 2>&1; then
        echo ""
        echo "============================================"
        echo "  ✅  系统启动成功！"
        echo "============================================"
        echo ""
        echo "  🌐 前端访问地址:"
        echo "     http://localhost:${FRONTEND_PORT}"
        echo "     http://127.0.0.1:${FRONTEND_PORT}"
        echo ""
        echo "  🔧 后端 API 地址:"
        echo "     http://localhost:${BACKEND_PORT}/api"
        echo "     http://127.0.0.1:${BACKEND_PORT}/api"
        echo ""
        echo "  💡 提示:"
        echo "     - 停止服务: ./stop.sh"
        echo "     - 查看日志: docker compose logs -f"
        echo ""
        exit 0
    fi
    RETRY=$((RETRY + 1))
    echo -n "."
    sleep 2
done

echo ""
echo "⚠️  服务启动超时，请检查容器状态: docker compose ps"
echo ""
docker compose ps
exit 1
