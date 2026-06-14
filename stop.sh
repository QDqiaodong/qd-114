#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

echo "============================================"
echo "  花卉种子培育跟踪系统 - 停止中"
echo "============================================"
echo ""

docker compose down

echo ""
echo "✅ 服务已停止"
echo ""
