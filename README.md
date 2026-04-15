# AI 简历优化系统

一个基于 Spring Boot + Spring AI Alibaba（DashScope/Qwen）的智能简历诊断与优化系统，包含完整的后端服务和前端界面。

## 系统架构

```
┌─────────────────────────────────────────────────────────────┐
│                      前端 (Vue 3)                           │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐       │
│  │ 简历优化  │ │ 简历诊断  │ │ 面试模拟  │ │ 知识库   │       │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘       │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    后端 (Spring Boot)                       │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐        │
│  │ 认证服务      │ │ 工作流引擎    │ │ RAG 服务     │        │
│  └──────────────┘ └──────────────┘ └──────────────┘        │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐        │
│  │ 简历解析      │ │ AI Agent     │ │ 文件服务     │        │
│  └──────────────┘ └──────────────┘ └──────────────┘        │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│              AI 能力层 (Spring AI Alibaba)                  │
│         DashScope / Qwen + RAG (pgvector)                   │
└─────────────────────────────────────────────────────────────┘
```

## 核心功能

- **简历诊断**：基于目标岗位分析简历问题，给出评分和改进建议
- **简历优化**：使用 AI 一键改写简历，提升专业度和匹配度
- **面试模拟**：AI 面试官进行针对性技术面试
- **Agent 工作流**：多 Agent 协作完成复杂任务（职位检测→诊断→优化）
- **RAG 知识库**：支持导入职位描述和写简历指南，增强 AI 回答质量
- **多格式支持**：PDF、Word、Markdown、HTML 简历解析和导出

## 项目结构

```
ai-cv-optimize/
├── src/main/java/com/luren/aicvoptimize/    # 后端源码
│   ├── agents/                              # AI Agent 定义
│   ├── controller/                          # REST API 接口
│   ├── service/                             # 业务逻辑
│   ├── workflow/                            # 工作流引擎
│   ├── rag/                                 # RAG 检索增强
│   ├── security/                            # JWT 认证
│   └── config/                              # 配置类
├── ai-cv-optimize-web/                      # 前端项目 (Vue 3)
│   ├── src/views/                           # 页面组件
│   ├── src/api/                             # API 封装
│   └── src/stores/                          # Pinia 状态管理
├── docker/                                  # Docker 配置
└── pom.xml                                  # Maven 配置
```

## 运行环境

- JDK：**17+（推荐 21）**
- Maven：3.8+
- 需要配置环境变量：`AI_DASHSCOPE_API_KEY`

> 如果你看到 `class file has wrong version 61.0, should be 52.0`，说明 Maven 正在用 **JDK8** 编译 Spring Boot 3.x；请切到 JDK17/21 后再运行。

## 启动

```bash
mvn -v
```

确认输出里 `Java version` 是 17 或 21。

然后：

```bash
mvn spring-boot:run
```

后端服务默认运行在 http://localhost:8080

## API 接口文档

### 认证接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/auth/login` | POST | 用户登录 |
| `/api/auth/register` | POST | 用户注册 |
| `/api/auth/me` | GET | 获取当前用户信息 |
| `/api/quota/remaining` | GET | 查询剩余使用次数 |

**登录请求示例：**
```json
POST /api/auth/login
{
  "username": "user",
  "password": "password"
}
```

**响应：**
```json
{
  "token": "jwt_token",
  "userId": 1,
  "username": "user",
  "email": "user@example.com",
  "nickname": "用户昵称"
}
```

### 简历核心接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/cv/health` | GET | 健康检查 |
| `/api/cv/diagnose` | POST | 简历诊断（JSON 输出） |
| `/api/cv/optimize` | POST | 简历优化改写（JSON 输出） |
| `/api/cv/parse` | POST | 简历文件解析 |

**简历诊断请求：**
```json
POST /api/cv/diagnose
{
  "resumeText": "你的简历文本……",
  "targetJobTitle": "Java后端开发工程师",
  "targetJobDescription": "岗位描述……（可选）",
  "seniority": "3-5年（可选）",
  "language": "zh-CN（可选）"
}
```

**简历优化请求：**
```json
POST /api/cv/optimize
{
  "resumeText": "你的简历文本……",
  "targetJobTitle": "Java后端开发工程师",
  "targetJobDescription": "岗位描述……（可选）",
  "seniority": "3-5年（可选）",
  "language": "zh-CN（可选）",
  "outputFormat": "markdown（可选：markdown/text）"
}
```

### 工作流接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/workflow/resume/start` | POST | 启动交互式优化工作流 |
| `/api/workflow/resume/resume` | POST | 恢复工作流执行 |
| `/api/workflow/resume/deliver` | POST | 交付模式（一键完成） |
| `/api/workflow/progress/{threadId}` | GET | 查询工作流进度 |

**启动工作流：**
```bash
POST /api/workflow/resume/start
Content-Type: multipart/form-data

file: 简历文件(PDF/Word)
```

### 面试接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/interview/start` | POST | 开始面试会话 |
| `/api/interview/answer` | POST | 提交面试回答 |
| `/api/interview/{sessionId}/next` | GET | 获取下一题 |
| `/api/interview/{sessionId}/evaluation` | GET | 获取面试评价 |

**开始面试：**
```json
POST /api/interview/start
{
  "resumeText": "简历内容",
  "jobTitle": "目标职位",
  "difficulty": "medium"
}
```

### RAG 知识库接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/rag/importText` | POST | 导入文本到知识库 |
| `/api/rag/importFile` | POST | 上传文件到知识库 |
| `/api/rag/query` | POST | 语义检索 |

### 文件下载接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/files/download/{filename}` | GET | 下载生成的简历文件 |
