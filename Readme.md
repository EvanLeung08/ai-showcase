# EVAN AI HUB

## 项目概述
EVAN AI HUB 是个人业余兴趣打造的集AI内容生成、文档处理、知识管理于一体的智能平台，整合多种AI能力提供以下核心价值：
- **多模态生成**：支持文本/PPT/PDF/图像生成与转换
- **智能解析**：自动处理书籍、文档、代码等结构化内容
- **知识中枢**：构建个性化知识库实现智能问答
- **开放集成**：支持Xunfei/DeepSeek/Ollama等多AI后端

![网站主页](/doc/14share.png)
![网站主页](/doc/14share_1.png)
![网站主页](/doc/14share_2.png)
![网站主页](/doc/14share_3.png)
![网站主页](/doc/14share_4.png)
![网站主页](/doc/14share_5.png)
![网站主页](/doc/14share_6.png)
![网站主页](/doc/14share_7.png)
![网站主页](/doc/14share_8.png)

## 核心功能模块

### 1. 智能内容中心
| 功能                | 技术实现                          | 示例场景                  |
|---------------------|----------------------------------|-------------------------|
| PPT智能生成         | Apache POI + Markdown解析        | 教学课件/商业提案自动生成 |
| PDF文档转换         | PDFBox + 高分辨率渲染引擎         | 跨平台文档格式转换        |
| AI绘图引擎          | 文生图API + Base64编码传输        | 插图/图表自动生成         |
| 多语言翻译          | 神经网络翻译(NMT)接口             | 文档实时多语言互译        |

### 2. 知识管理中枢
| 模块                | 技术亮点                          | 应用场景                  |
|---------------------|----------------------------------|-------------------------|
| 书籍笔记生成        | PDF解析 + 结构化模板              | 学术文献自动摘要          |
| 代码知识库          | 向量化存储 + 语义检索             | 开发文档智能问答          |
| 智能写作助手        | Copilot API集成 + 上下文记忆       | 技术文档辅助生成          |
| 记忆强化系统        | 间隔重复算法 + 知识图谱            | 学习内容长期记忆管理      |

### 3. 开发者功能栈
| 服务                | 技术组件                          | 调用方式                  |
|---------------------|----------------------------------|-------------------------|
| REST API网关         | Spring MVC + 统一异常处理         | 第三方系统集成            |
| 模型路由            | 多AI提供商动态调度                | Xunfei/DeepSeek无缝切换   |
| 提示词工程          | 模板引擎 + 上下文注入              | 生成结果精准控制          |
| 监控中心            | 日志埋点 + 性能指标采集            | 服务健康状态追踪          |

## 技术架构
```bash
.
├── AI服务层
│   ├── Xunfei Provider    # 讯飞星火能力集成
│   ├── DeepSeek Provider  # DeepSeek大模型接口
│   └── Ollama Provider    # 本地模型服务适配
├── 核心引擎
│   ├── 文档解析引擎       # PDF/Markdown/PPT处理
│   ├── 向量计算引擎       # 文本向量化与相似度计算
│   └── 工作流引擎        # 复杂任务流水线管理
├── 前端界面
│   ├── Bootstrap 5       # 响应式UI框架
│   ├：：arked.js         # Markdown实时渲染
│   └：：art.js           # 数据可视化图表
└── 基础设施
    ├── Spring Boot 3.x   # 后端主框架
    ├── Apache POI        # Office文档操作
    └── PDFBox            # PDF处理库
```

## 快速开始
```bash
# 1. 克隆仓库
git clone https://github.com/your-repo/evan-ai-hub.git

# 2. 配置AI密钥 (可选)
cp config-template.properties src/main/resources/application.properties

# 3. 编译运行
mvn clean install
mvn spring-boot:run

# 4. 访问平台
https://localhost:8080
```

# 开发者指南
## API文档结构
```java
POST /api/generate/ppt    # PPT生成接口
POST /api/knowledge/upload  # 知识库上传 
GET /api/copilot/chat     # 智能问答接口
POST /api/translate       # 多语言翻译接口

```
## 扩展开发示例
```java
public class CustomProvider implements AiApiProvider {
    @Override
    public String generateContent(String prompt, String systemRole) {
        // 实现自定义AI模型接入
        return customAI.generate(prompt); 
    }
}

```
# 许可证
本项目采用 Apache 2.0 开源协议，商业使用需遵守各AI服务商的授权条款。
```plaintext

该文档特点：
1. 突出平台化特性而非单一功能
2. 技术实现与业务场景结合展示
3. 开发者友好型架构说明
4. 模块化展示扩展能力
5. 强调多AI后端支持特性

```