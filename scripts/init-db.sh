#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

echo "📊 初始化 FaceSend 数据库..."
docker exec -i mysql mysql -uroot -p123456 < "$PROJECT_ROOT/database/init.sql"
echo "✅ 数据库初始化完成！"
