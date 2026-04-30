#!/bin/bash

echo "🛑 停止 FaceSend 环境..."

docker stop mysql 2>/dev/null && echo "✅ MySQL 已停止"
docker stop minio 2>/dev/null && echo "✅ MinIO 已停止"

echo "所有服务已停止！"
