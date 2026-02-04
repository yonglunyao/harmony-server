# Harmony Server

> HarmonyOS 文件服务器 - 提供文件上传、下载、删除等功能的 RESTful API 服务

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

## 功能特性

### 文件管理
- 📤 **文件上传** - 支持大文件上传（最大 2GB）
- 📥 **文件下载** - 支持按文件名或路径下载
- 🗑️ **文件删除** - 支持单个文件或目录删除
- 🧹 **清空目录** - 一键清空上传目录
- 📋 **文件列表** - 查看所有文件或按路径查询

### 数据接口
- 📊 **数据上报** - 设备数据收集接口
- 📝 **日志上报** - 应用日志上报接口
- 💓 **心跳检测** - 设备在线状态检测

### 安全特性
- 🔒 **路径遍历防护** - 防止 `../` 等路径遍历攻击
- 📁 **目录限制** - 只能操作 `uploads` 目录下的内容
- 🛡️ **根目录保护** - 禁止删除上传根目录

## 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 17 | 编程语言 |
| Spring Boot | 3.2.0 | 应用框架 |
| Maven | - | 构建工具 |
| JUnit 5 | - | 测试框架 |

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+

### 运行服务

```bash
# 克隆仓库
git clone https://github.com/yonglunyao/harmony-server.git
cd harmony-server

# 编译运行
mvn spring-boot:run
```

服务将在 `http://localhost:8877` 启动。

### 打包部署

```bash
# 打包
mvn clean package

# 运行 JAR
java -jar target/harmony-server-1.0-SNAPSHOT.jar
```

## API 文档

### 基础信息

| 项目 | 内容 |
|------|------|
| 基础地址 | `http://localhost:8877` |
| 编码格式 | UTF-8 |

### 文件接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/file/upload` | POST | 上传文件 |
| `/api/file/delete/{filename}` | POST | 删除文件（按文件名） |
| `/api/file/delete/path` | POST | 删除文件（按路径） |
| `/api/file/clean` | POST | 清空上传目录 |
| `/api/file/download/{filename}` | GET | 下载文件（按文件名） |
| `/api/file/download/path` | GET | 下载文件（按路径） |
| `/api/file/list` | GET | 列出所有文件 |
| `/api/file/list/path` | GET | 列出文件（按路径） |

### 数据接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/data/report` | POST | 上报设备数据 |
| `/api/data/log` | POST | 上报日志 |
| `/api/data/heartbeat` | POST | 心跳检测 |

### 请求示例

#### 上传文件

```bash
curl -X POST http://localhost:8877/api/file/upload \
  -F "file=@/path/to/file.exe" \
  -F "category=tools"
```

#### 删除文件

```bash
curl -X POST http://localhost:8877/api/file/delete/file.exe
```

#### 下载文件

```bash
curl -O http://localhost:8877/api/file/download/file.exe
```

#### 查看文件列表

```bash
curl http://localhost:8877/api/file/list
```

#### 清空目录

```bash
curl -X POST http://localhost:8877/api/file/clean
```

## 配置说明

### application.yml

```yaml
server:
  port: 8877
  tomcat:
    max-swallow-size: -1
    max-http-form-post-size: -1

spring:
  servlet:
    multipart:
      max-file-size: 2GB
      max-request-size: 2GB
```

### 大文件上传支持

- **单个文件**: 最大 2GB
- **请求大小**: 最大 2GB

如需支持更大文件，修改配置并重启服务。

## 测试

### 运行测试

```bash
# 运行所有测试
mvn test

# 运行指定测试类
mvn test -Dtest=FileControllerTest
```

### 测试覆盖

```
Tests run: 40, Failures: 0, Errors: 0, Skipped: 0
```

- FileControllerTest: 29 个测试
- DataControllerTest: 10 个测试
- AppTest: 1 个测试

## Postman 集合

项目包含完整的 Postman Collection，导入后可直接测试所有接口。

**导入文件**: `postman-collection.json`

### 导入步骤

1. 打开 Postman
2. 点击 Import → 选择文件
3. 导入 `postman-collection.json`
4. 修改环境变量：
   - `baseUrl`: 服务器地址
   - `filePath`: 本地测试文件路径

## 目录结构

```
harmony-server/
├── src/
│   ├── main/
│   │   ├── java/org/example/
│   │   │   ├── App.java                    # 启动类
│   │   │   └── controller/
│   │   │       ├── FileController.java     # 文件接口
│   │   │       └── DataController.java     # 数据接口
│   │   └── resources/
│   │       └── application.yml             # 配置文件
│   └── test/
│       └── java/org/example/
│           └── controller/                 # 测试代码
├── uploads/                                 # 上传目录
├── postman-collection.json                  # API 集合
├── pom.xml                                  # Maven 配置
└── README.md                                # 项目文档
```

## 安全说明

### 路径遍历防护

所有文件操作都经过安全验证：

- ✅ 路径规范化处理
- ✅ 禁止访问 uploads 目录外的文件
- ✅ 禁止删除 uploads 根目录
- ✅ 文件名安全检查

### 访问控制

当前版本未实现用户认证，建议在生产环境前添加：
- JWT/OAuth2 认证
- API 密钥验证
- IP 白名单

## 部署

### 本地部署

```bash
java -jar target/harmony-server-1.0-SNAPSHOT.jar
```

### Docker 部署

```dockerfile
FROM openjdk:17-slim
COPY target/harmony-server-1.0-SNAPSHOT.jar app.jar
EXPOSE 8877
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

### Linux 后台运行

```bash
# 使用 nohup
nohup java -jar harmony-server.jar > app.log 2>&1 &

# 使用 systemd
sudo systemctl start harmony-server
```

## 开源协议

[MIT License](LICENSE)

## 作者

[yonglunyao](https://github.com/yonglunyao)

## 许可证

MIT License

---

⭐ 如果这个项目对你有帮助，请给个 Star！
