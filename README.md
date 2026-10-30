# 商品管理系统（goods）

基于 **Spring Boot 3 + MyBatis-Plus + Thymeleaf** 的电商 / 商品管理系统，包含**买家前台**、**卖家中心**与**管理后台**三种角色。

## 功能概览

- **用户体系**：注册、登录、JWT 鉴权、角色区分（买家 / 卖家 / 管理员）
- **商品管理**：商品发布、编辑、上下架、定时上架、图片上传、分类
- **购物车与订单**：加购、下单、订单列表、订单详情与状态流转
- **内容模块**：文章发布、文章列表、文章详情与评论
- **卖家中心**：卖家仪表盘、商品管理、订单管理
- **管理后台**：数据看板、用户管理、商品管理、订单管理、评论管理、文章管理
- **系统能力**：Druid 连接池与 SQL 监控、Redis 缓存、RabbitMQ 异步通知、邮件通知、全局异常页

## 技术栈

| 层次 | 技术 |
|---|---|
| 语言 / 框架 | Java 21、Spring Boot 3.4.5 |
| 持久层 | MyBatis-Plus 3.5.5、MySQL 9.x 驱动、Druid 1.2.18 |
| 中间件 | Redis、RabbitMQ |
| 视图 | Thymeleaf 模板（`src/main/resources/templates`） |
| 工具 | Lombok、Hutool 5.8.23 |
| 安全 | Spring Security + JWT |

## 目录结构

```
sql/schema.sql     # 建表脚本 + 演示数据（13 张表）
src/main/java/com/example/goods/
  controller/      # 前台、卖家、后台、认证、文件上传等 18 个控制器
  service/         # 业务层
  mapper/          # MyBatis-Plus Mapper
  goods/           # 实体与 VO
  config/          # JWT 过滤器、Security、MyBatis-Plus 等配置
  common/          # JwtUtil、统一响应
src/main/resources/
  templates/       # Thymeleaf 页面（admin / seller / 前台 / 错误页）
  static/img/      # 商品图片资源
  application.yml  # 配置（敏感项已改为环境变量占位）
pom.xml / mvnw     # Maven 工程
```

## 快速开始

### 1. 准备环境

- JDK 21、Maven（或用自带 `mvnw`）
- MySQL 8.x
- Redis（默认 `localhost:6379`）
- RabbitMQ（默认 `localhost:5672`，guest/guest）

### 2. 初始化数据库

```bash
mysql -u root -p < sql/schema.sql
```

脚本会自动创建 `shutiao` 库、13 张表，并写入演示数据（用户、分类、9 件商品、文章、评论），可重复执行。

### 3. 配置并启动

`application.yml` 中的敏感项为环境变量占位，未注入时使用括号内默认值：

| 变量 | 说明 | 默认值 |
|---|---|---|
| `DB_USERNAME` / `DB_PASSWORD` | MySQL 账号密码 | `root` / `your_mysql_password` |
| `DRUID_USERNAME` / `DRUID_PASSWORD` | Druid 监控台账号密码 | `admin` / `your_druid_password` |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | 发信邮箱与授权码 | 占位符 |
| `MAIL_ENABLED` | 是否启用邮件通知 | `false` |
| `NOTIFY_EMAIL` | 通知收件人 | 占位符 |
| `RABBITMQ_USERNAME` / `RABBITMQ_PASSWORD` | MQ 账号密码 | `guest` / `guest` |

```bash
export DB_PASSWORD=你的数据库密码
./mvnw spring-boot:run
```

### 4. 访问入口

- 前台首页：<http://localhost:8081/>
- 登录 / 注册：`/login`、`/register`
- 管理后台：`/admin/dashboard`
- 卖家中心：`/seller/dashboard`
- Druid 监控：`/druid/*`

演示账号（来自 `sql/schema.sql`，密码为明文方便体验，库中存 BCrypt 哈希）：

| 账号 | 密码 | 角色 |
|---|---|---|
| `admin` | `admin123` | 管理员 ADMIN |
| `seller1` | `seller123` | 卖家 SELLER |
| `user1` | `user123` | 普通用户 USER |

## 说明

- 建表脚本不含外键约束，避免演示时删改商品/订单被约束阻塞。
- `frontend/` 为早期 Vue 3 实验版本，已废弃，未包含在演示包中；可演示界面为 Spring Boot + Thymeleaf 页面。
