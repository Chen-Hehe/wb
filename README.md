# 微博全栈系统 (Weibo Full-Stack System)

基于 Spring Boot 3.x + React 18 + MySQL 的微博系统

## 技术栈

### 后端
- **框架**: Spring Boot 3.2.4
- **ORM**: MyBatis-Plus 3.5.5
- **数据库**: MySQL 8.0+
- **认证**: JWT (io.jsonwebtoken 0.12.5)
- **安全**: Spring Security 6
- **文档**: SpringDoc OpenAPI 2.3.0

### 前端
- **框架**: React 18.2
- **构建工具**: Vite 5.1
- **语言**: TypeScript 5.2
- **UI 组件**: Ant Design 5.15
- **路由**: React Router DOM 6.22
- **HTTP 客户端**: Axios 1.6.7

## 项目结构

```
weibo-system/
├── init.sql                 # 数据库初始化脚本
├── backend/                 # Spring Boot 后端
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/weibo/
│       │   ├── WeiboApplication.java
│       │   ├── common/      # 通用类
│       │   ├── config/      # 配置类
│       │   ├── controller/  # 控制器
│       │   ├── dto/         # 数据传输对象
│       │   ├── entity/      # 实体类
│       │   ├── exception/   # 异常处理
│       │   ├── filter/      # 过滤器
│       │   ├── mapper/      # MyBatis Mapper
│       │   └── service/     # 服务层
│       └── resources/
│           └── application.yml
├── frontend/                # React 前端
│   ├── package.json
│   ├── vite.config.ts
│   ├── tsconfig.json
│   └── src/
│       ├── api/            # API 接口
│       ├── pages/          # 页面组件
│       ├── utils/          # 工具函数
│       └── App.tsx
└── README.md
```

## 快速开始

### 1. 数据库配置

#### 安装 MySQL 8.0+

确保已安装 MySQL 8.0 或更高版本。

#### 初始化数据库

```bash
# 登录 MySQL
mysql -u root -p

# 执行初始化脚本
source /path/to/weibo-system/init.sql
```

或者使用 MySQL Workbench 等工具执行 `init.sql` 文件。

### 2. 后端配置

#### 修改数据库配置

编辑 `backend/src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/weibo_db?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root      # 修改为你的数据库用户名
    password: root      # 修改为你的数据库密码
```

#### 启动后端

```bash
cd backend

# Windows
mvnw.cmd clean compile

# 运行项目
mvnw.cmd spring-boot:run
mvn spring-boot:run

# 或者打包后运行
mvnw.cmd clean package
java -jar target/weibo-backend-1.0.0.jar
```

后端服务将在 `http://localhost:8080/api/v1` 启动。

### 3. 前端配置

#### 安装依赖

```bash
cd frontend
npm install
```

#### 启动开发服务器

```bash
npm run dev
```

前端服务将在 `http://localhost:5173` 启动。

## API 文档

启动后端后，访问以下地址查看 API 文档：

- **Swagger UI**: http://localhost:8080/api/v1/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/api/v1/v3/api-docs

## API 接口

### 认证接口

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | /auth/register | 用户注册 |
| POST | /auth/login | 用户登录 |
| POST | /auth/refresh | 刷新 Token |

### 用户接口

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | /users/current | 获取当前用户信息 |
| GET | /users/{userId} | 获取用户信息 |
| PUT | /users | 更新用户信息 |
| POST | /users/{userId}/follow | 关注用户 |
| DELETE | /users/{userId}/follow | 取消关注 |
| GET | /users/{userId}/following | 获取关注列表 |
| GET | /users/{userId}/followers | 获取粉丝列表 |

### 微博接口

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | /weibos | 发布微博 |
| DELETE | /weibos/{weiboId} | 删除微博 |
| GET | /weibos/{weiboId} | 获取微博详情 |
| GET | /weibos | 获取微博列表 |
| GET | /weibos/user/{userId} | 获取用户微博列表 |
| GET | /weibos/following | 获取关注的微博 |
| POST | /weibos/{weiboId}/like | 点赞微博 |
| DELETE | /weibos/{weiboId}/like | 取消点赞 |
| POST | /weibos/{weiboId}/repost | 转发微博 |

### 评论接口

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | /comments | 发表评论 |
| DELETE | /comments/{commentId} | 删除评论 |
| GET | /comments/weibo/{weiboId} | 获取微博评论 |
| GET | /comments/{commentId}/replies | 获取子评论 |
| POST | /comments/{commentId}/like | 点赞评论 |
| DELETE | /comments/{commentId}/like | 取消点赞 |

## 测试账号

数据库初始化后，可以使用以下测试账号登录：

| 用户名 | 密码 | 邮箱 |
|--------|------|------|
| admin | 123456 | admin@weibo.com |
| user1 | 123456 | user1@weibo.com |
| user2 | 123456 | user2@weibo.com |

## 配置说明

### JWT 配置

在 `application.yml` 中配置 JWT 相关参数：

```yaml
jwt:
  secret: your-secret-key-here  # JWT 密钥
  expiration: 86400000          # Token 有效期（毫秒）
  refresh-expiration: 604800000 # 刷新 Token 有效期（毫秒）
```

### CORS 配置

默认允许 `http://localhost:5173` 跨域访问，如需修改请编辑 `CorsConfig.java`。

## 常见问题

### 1. 后端启动失败

- 检查 MySQL 是否正常运行
- 检查数据库用户名密码是否正确
- 检查端口 8080 是否被占用

### 2. 前端无法连接后端

- 确保后端服务已启动
- 检查 `vite.config.ts` 中的代理配置
- 检查浏览器控制台是否有 CORS 错误

### 3. 登录失败

- 确认数据库中有测试数据
- 检查密码是否正确（默认密码：123456）
- 查看后端日志获取详细错误信息

## 开发说明

### 后端开发

- 使用 Lombok 简化代码
- 遵循 RESTful API 设计规范
- 使用 MyBatis-Plus 进行数据库操作
- 全局异常处理在 `GlobalExceptionHandler`

### 前端开发

- 使用 TypeScript 进行类型检查
- 使用 Ant Design 组件库
- API 请求统一在 `src/api/` 目录管理
- 使用 Axios 拦截器处理 Token 和错误

## 许可证

MIT License

## 联系方式

如有问题，请提交 Issue 或联系开发者。
