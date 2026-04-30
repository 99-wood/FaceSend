#!/bin/bash

echo "🚀 FaceSend 环境启动中..."

# 检查 MySQL 是否在运行
if [ "$(docker ps -aq -f name=mysql)" ] && [ -z "$(docker ps -q -f name=mysql)" ]; then
    echo "▶️  启动已存在的 MySQL 容器..."
    docker start mysql
elif [ -z "$(docker ps -aq -f name=mysql)" ]; then
    echo "🆕 创建并启动 MySQL 8.0..."
    docker run -d -p 3306:3306 --name mysql -e MYSQL_ROOT_PASSWORD=123456 mysql:8.0
else
    echo "✅ MySQL 已运行"
fi

# 检查 MinIO 是否在运行
if [ "$(docker ps -aq -f name=minio)" ] && [ -z "$(docker ps -q -f name=minio)" ]; then
    echo "▶️  启动已存在的 MinIO 容器..."
    docker start minio
elif [ -z "$(docker ps -aq -f name=minio)" ]; then
    echo "🆕 创建并启动 MinIO..."
    docker run -d -p 9000:9000 -p 9001:9001 --name minio minio/minio server /data --console-address ":9001"
else
    echo "✅ MinIO 已运行"
fi

echo ""
echo "🎉 所有服务启动完成！"
echo "   MySQL:  localhost:3306  (root / 123456)"
echo "   MinIO:  localhost:9001 (minioadmin / minioadmin)"
echo ""
