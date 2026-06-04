#!/usr/bin/env bash
set -e

cd "$(dirname "$0")"

echo "========================================="
echo "  刷题助手 - 生产部署"
echo "========================================="

# 1. 安装依赖（如需要）
if [ ! -d "node_modules" ]; then
  echo ""
  echo "[1/2] 安装依赖..."
  npm install
else
  echo ""
  echo "[1/2] 依赖已存在，跳过安装"
fi

# 2. 构建
echo ""
echo "[2/2] 构建生产包..."
npm run build

# 3. 启动
echo ""
echo "========================================="
echo "  启动服务: http://0.0.0.0:3002"
echo "========================================="
echo ""
npm start
