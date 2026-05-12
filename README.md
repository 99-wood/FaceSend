# FaceSend

> 输入相同的码，面对面传文件。

FaceSend 是一款基于 Android 平台的面对面文件传输应用：发送者创建房间得到 6 位码，接收者输入相同码并在 200m 范围内即可加入，房间内可双向传输文件，关闭后服务器立即清理。无需注册、无需好友、用完即走。

## 功能特性

- 6 位数字码 + GPS 距离校验，避免远程误匹配
- 支持图片、文档、视频、压缩包等常见格式，单文件最大 100MB
- 上传/下载实时进度，断点续传友好
- 房间内成员列表 + 昵称系统（设置页持久化、房间内可改临时昵称）
- 房主可控制"允许成员上传"权限
- 30 分钟无活动自动清理，房间关闭后文件即时删除

## 技术栈

| 端 | 技术 |
|----|------|
| Android | Kotlin + Jetpack Compose + Retrofit + Google Play Services |
| 后端 | Spring Boot 3.4 + Java 21 + JPA |
| 存储 | MySQL 8.0（元数据）+ MinIO（文件） |
| 部署 | Docker Compose |

## 目录结构

```
FaceSend/
├── android/         Android 客户端（Kotlin + Compose）
├── server/          Spring Boot 后端
├── database/        数据库初始化脚本
├── deploy/          完整 docker-compose 部署
├── deploy-minimal/  轻量分步部署方案
└── scripts/         本地启动/停止脚本
```

## 快速启动

### 后端（本地）

```bash
# 启动 MySQL + MinIO + 后端
cd deploy && docker compose up -d
```

或本地单独启动：

```bash
cd server && ./mvnw spring-boot:run
```

### Android

用 Android Studio 打开 `android/` 目录，配置 `local.properties` 中的 SDK 路径，连接设备直接运行。  
默认服务端地址在 `data/RetrofitClient.kt`，请按实际部署修改。

## 测试

```bash
cd server && ./mvnw test
```

- `RoomServiceUnitTest` — 11 个用例，覆盖核心业务逻辑
- `RoomControllerIntegrationTest` — 8 个用例，完整 HTTP 链路
- `FileControllerIntegrationTest` — 文件接口验证

## 团队分工

| 成员 | 负责模块 |
|------|----------|
| zjd  | 项目搭建、房间核心服务、数据库、测试 |
| fw   | MinIO 文件接口、部署 |
| lxy  | Android UI（首页/房间页/设置/主题） |
| zsj  | Android 数据层、网络、文件工具 |
