# 贡献指南

感谢你对 CourseHelper 项目的兴趣！我们欢迎各种形式的贡献。

## 如何开始

### 前置需求

- Kotlin 开发环境
- JDK 17 或更高版本
- Android SDK（如适用）
- Git

### 克隆仓库

```bash
git clone https://github.com/CookiesHax/CourseHelper.git
cd CourseHelper
```

### 开发环境设置

1. 导入项目到 IDE（如 Android Studio 或 IntelliJ IDEA）
2. 同步 Gradle 文件
3. 构建项目以确保环境正确

## 项目结构说明

本项目采用模块化架构，主要代码位于 `app/src/main/java/com/cookieshax/coursehelper/`：

- **`app/`**: 宿主模块
    - 应用程序入口 (`Application`)。
    - 全局导航逻辑与主界面 (`MainActivity`)。
    - 全局状态管理。

- **`core/`**: 核心基础能力 (指向上层业务的支撑)
    - `database/`: 数据库定义及 DAO (Room)。
    - `network/`: 网络请求封装、API Service 及拦截器。
    - `repository/`: **数据仓库层**，负责整合网络与本地数据，是业务逻辑的核心。
    - `location/`: 带有模拟定位数据的定位服务封装。
    - `permission/`: 权限申请封装。
    - `utils/`: 各种通用工具类（日期、加密、字符串处理等）。

- **`feature/`**: 业务功能模块 (按功能垂直划分)
    - 每个子包（如 `login`, `checkin`, `course`）是一个独立的功能域。
    - 遵循 MVVM 模式：内部包含 `UI` (Compose/Fragment/Activity) 与 `ViewModel`。
    - 特殊模块：`camera` (扫码), `map` (地图展示), `webview` (网页容器)。

- **`ui/`**: 表现层公共组件
    - `theme/`: 全局配色、字体及 Compose 主题配置。
    - 公用的 UI 组件（如自定义 Button、Dialog 等）。

## 贡献流程

### 1. 创建 Issue

在提交代码前，请先创建一个 Issue 来讨论：

- 新功能需求
- Bug 报告
- 性能改进
- 文档改进

请在 Issue 中清晰描述问题或需求。**提示：避免 [XY 问题](https://xyproblem.info/)**
，请直接描述你的最终目标，而不是描述你在尝试解决该目标时遇到的中间技术问题。

### 2. Fork 并创建分支

```bash
git checkout -b feature/your-feature-name
```

分支命名规范：

- feature/feature-name - 新功能
- bugfix/bug-name - 修复 Bug
- docs/description - 文档更新
- refactor/description - 代码重构

### 3. 编写代码

- 遵循 Kotlin 官方编码规范
- 添加必要的注释和文档
- 为新功能编写测试
- 确保代码在本地编译无误

### 4. 提交 Pull Request

提交 PR 时，请：

- 清晰描述改动内容
- 关联相关的 Issue（如果有）
- 包含测试用例或截图（如适用）
- 遵循现有代码风格

PR 标题格式：

- feat: 添加新功能
- fix: 修复 Bug
- docs: 更新文档
- refactor: 代码重构

### 5. 代码审查

维护者会审查你的 PR。请：

- 及时响应反馈
- 根据建议进行修改
- 保持与主分支的同步

## 代码规范

### Kotlin 规范

- 使用 4 空格缩进
- 遵循 Kotlin 命名约定
- 避免使用非必要的全局变量
- 优先使用 Kotlin 特性而非 Java 特性

### 提交信息规范

遵循 [Conventional Commits](https://www.conventionalcommits.org/) 格式

```
<type>[optional scope]: <description>

[optional body]

[optional footer(s)]
```

示例：

```
feat(auth): 添加登录验证功能

实现了用户登录验证，包括密码检查和会话管理。

Closes #123
```

## 测试

- 为新功能编写单元测试
- 测试应覆盖主要逻辑和边界情况
- 所有测试必须通过后才能提交 PR

运行测试：

```bash
./gradlew test
```

## 文档

- 更新 README.md（如功能改变了用法）
- 为复杂功能添加注释
- 如需新增功能，请更新相关文档

## 行为准则

请参阅我们的 [行为准则](CODE_OF_CONDUCT.md)。

## 许可证

通过贡献代码，你同意你的贡献将在项目的现有许可证下发布。

## 联系方式

如有问题，可以：

- 在 Issue 中留言
- 提交讨论
- 联系项目维护者

感谢你的贡献！🎉
