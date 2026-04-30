@echo off
echo 🛑 停止 FaceSend 环境...

docker stop mysql 2>nul
docker stop minio 2>nul

echo 所有服务已停止！
pause
