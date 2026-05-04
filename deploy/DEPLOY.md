# FaceSend 远程服务器部署指南

## 第一步：环境准备（你的远程服务器需要预装）
```bash
# Ubuntu/Debian 系统
sudo apt update && sudo apt install -y docker.io docker-compose openjdk-21-jdk maven git
sudo systemctl enable --now docker
```

## 第二步：打包后端
在你本地 Windows 的 server 目录下执行：
```bash
cd d:\code\project\FaceSend\server
mvn clean package -DskipTests
```
生成的文件就在 `server/target/facesend-0.0.1-SNAPSHOT.jar`，你把它重命名为 `facesend-server.jar`

## 第三步：上传文件到远程服务器
```bash
# 用 scp 上传
scp -r d:\code\project\FaceSend root@你的服务器IP:/root/FaceSend
```

## 第四步：服务器上一键启动
```bash
ssh root@你的服务器IP
cd /root/FaceSend/deploy
docker-compose up -d
```

## 第五步：初始化数据库
```bash
cd /root/FaceSend
docker exec -i facesend-mysql mysql -uroot -p123456 < database/init.sql
```

## 第六步：配置 Android App
把你 Android 代码里 `RetrofitClient.kt` 的 BASE_URL 改成你的服务器公网 IP 就行，比如：
```kotlin
private const val BASE_URL = "http://你的服务器公网IP:8080/"
```

完成！整个 FaceSend 就部署到云服务器上了，全球任何地方的人都可以用！
