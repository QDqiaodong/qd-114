# 🌸 家用花卉种子培育跟踪系统

全程跟踪花卉播种培育流程，记录种子存放、播种、萌芽、长苗、移栽等全周期状态。

## ✨ 功能特性

- 🌱 **种子信息登记** - 花卉品种、购入/采收时间、存放位置、初始数量管理
- 🌾 **播种操作记录** - 播种时间、盆土配比、覆土厚度、初期浇水方案
- 🌿 **生长阶段跟踪** - 萌芽、展叶、成苗、根系发育等生长变化时序记录
- 🪴 **移栽分盆记录** - 移栽时间、花盆规格、缓苗养护要点
- 📊 **首页数据概览** - 统计卡片、培育进度时间线、养护小贴士
- 🏷️ **阶段标签批量绑定** - 标准培育阶段可视化进度展示
- ⚡ **Redis 缓存** - 列表结构缓存标准培育流程和品种数据

## 🛠️ 技术栈

### 前端
- **框架**: Vue 3 + Vite
- **UI 组件**: Element Plus
- **路由**: Vue Router 4（懒加载）
- **HTTP**: Axios
- **日期处理**: Day.js

### 后端
- **框架**: Spring Boot 3.3
- **JDK**: Java 17
- **ORM**: Spring Data JPA
- **缓存**: Spring Data Redis
- **数据库**: MySQL 8.0

### 部署
- **容器化**: Docker + Docker Compose
- **前端部署**: Nginx
- **数据库**: MySQL 8.0（关闭二进制日志）
- **缓存**: Redis 7

## 📦 端口配置

| 服务 | 端口 | 说明 |
|------|------|------|
| 前端 Nginx | 3008 | 页面访问 |
| 后端 API | 8088 | 接口服务 |
| MySQL | 3309 | 数据库 |
| Redis | 6380 | 缓存服务 |

> 所有端口统一在 `.env` 文件中配置管理。

## 🚀 快速开始

### 环境要求
- Docker 20.10+
- Docker Compose v2+

### 启动服务

```bash
# 一键启动（推荐）
./start.sh

# 或使用 docker compose
docker compose up -d --build
```

启动成功后，访问：
- 🌐 前端页面: http://localhost:3008
- 🔧 后端 API: http://localhost:8088/api/health

### 停止服务

```bash
# 一键停止
./stop.sh

# 或使用 docker compose
docker compose down
```

## 📁 项目结构

```
.
├── .env                     # 全局环境变量配置
├── docker-compose.yml       # Docker Compose 编排
├── start.sh                 # 启动脚本
├── stop.sh                  # 停止脚本
├── README.md                # 项目说明
├── backend/                 # 后端 Spring Boot
│   ├── Dockerfile
│   ├── pom.xml              # Maven 配置（京东镜像源）
│   └── src/main/
│       ├── java/com/flower/cultivation/
│       │   ├── common/      # 通用类
│       │   ├── config/      # 配置类
│       │   ├── controller/  # 控制器
│       │   ├── entity/      # 实体类
│       │   ├── repository/  # 数据访问
│       │   ├── service/     # 业务逻辑
│       │   └── exception/   # 异常处理
│       └── resources/
│           ├── application.yml
│           └── application-docker.yml
├── frontend/                # 前端 Vue3
│   ├── Dockerfile
│   ├── nginx.conf           # Nginx 配置
│   ├── vite.config.js       # Vite 配置
│   ├── package.json         # npm 配置（华为云镜像）
│   └── src/
│       ├── api/             # API 接口
│       ├── views/           # 页面组件
│       ├── router/          # 路由
│       ├── utils/           # 工具函数
│       └── styles/          # 全局样式
├── mysql/                   # MySQL 配置
│   ├── conf/my.cnf          # 数据库配置
│   └── init/                # 初始化脚本
└── redis/
    └── redis.conf           # Redis 配置
```

## 🔧 配置说明

### 环境变量 (.env)

```bash
# Docker 镜像源（全局统一）
DOCKER_REGISTRY=docker.io

# 端口配置
FRONTEND_PORT=3008
BACKEND_PORT=8088
MYSQL_PORT=3309
REDIS_PORT=6380

# 数据库配置
MYSQL_ROOT_PASSWORD=flower123456
MYSQL_DATABASE=flower_cultivation
```

### Docker 构建缓存策略

- **前端**: `package.json` 单独作为缓存层，依赖无变更时复用构建缓存
- **后端**: `pom.xml` 单独作为缓存层，依赖无变更时跳过 mvn dependency:go-offline
- 仅使用 Docker 原生分层缓存机制，不引入额外语法
- 所有基础镜像统一使用 `DOCKER_REGISTRY` 前缀

### 国内镜像源

- **npm**: 华为云镜像 `https://mirrors.huaweicloud.com/repository/npm/`
- **Maven**: 京东开源镜像 `https://maven.jd.com/repository/public/`

## 📊 数据库设计

### 核心数据表

| 表名 | 说明 |
|------|------|
| flower_variety | 花卉品种表 |
| growth_stage | 培育阶段表 |
| seed_info | 种子信息表 |
| sowing_record | 播种记录表 |
| growth_tracking | 生长阶段跟踪表 |
| transplant_record | 移栽分盆记录表 |

## 🐳 容器命名规范

所有容器名称与项目名称保持一致：

- `flower-cultivation-mysql`
- `flower-cultivation-redis`
- `flower-cultivation-backend`
- `flower-cultivation-frontend`

## 🔒 安全说明

- 所有服务仅绑定 `127.0.0.1`，不对外暴露
- MySQL 使用非默认端口 3309
- Redis 使用非默认端口 6380
- 数据库初始密码请在 `.env` 中修改

## 📝 开发说明

### 前端本地开发

```bash
cd frontend
npm install
npm run dev
```

开发服务器默认运行在 `http://127.0.0.1:3008`

### 后端本地开发

```bash
cd backend
mvn spring-boot:run
```

后端服务默认运行在 `http://127.0.0.1:8088`

## 📄 License

MIT
