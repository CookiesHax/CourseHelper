# CourseHelper

一个基于 Kotlin 的学习通辅助工具，专注于提供更原生的 Android 移动端体验

## 鸣谢

特别感谢 [AneryCoft](https://github.com/AneryCoft)
及其项目 [course_helper](https://github.com/AneryCoft/course_helper)。

本项目在 API 端点请求结构以及关键加密算法的逻辑移植上，得到了该项目的参考，特此表达诚挚的谢意。

## 声明

本项目仅仅是出于练习目的和个人使用，不提供任何编译后的 Release 版本，也不提供任何形式的技术支持。
我之所以做这个是因为对相似的项目 UI/UX 不是很满意，故在业余时间开发了本项目。

## 构建指南

本项目使用 `local.properties` 管理私密配置，请确保在构建前已在根目录创建该文件。

注意：请勿将包含真实密钥的 `local.properties` 提交至公共仓库。

### 1. 配置定位 SDK

为了使应用内的定位功能正常工作，请在 `local.properties` 文件中添加以下配置：

```properties
# 百度地图/定位 SDK 密钥
BAIDU_API_KEY=你的百度API_KEY
```

### 2. 配置应用签名(可选)

在 `local.properties` 中添加签名相关配置以支持 Release 构建：

```properties
RELEASE_STORE_FILE=你的密钥库文件路径（如：/path/to/your/keystore.jks）
RELEASE_STORE_PASSWORD=你的密钥密码
RELEASE_KEY_ALIAS=你的密钥别名
RELEASE_KEY_PASSWORD=你的密钥密码
```

如果未提供上述签名配置，构建依然会以 Release 模式进行，但 Gradle 会自动回退使用 Debug 签名 对导出的
APK 进行签名。

## 许可与免责声明

### 1. 知识共享许可协议

本项目采用 [GNU General Public License v3.0 (GPL-3.0)](https://www.gnu.org/licenses/gpl-3.0.html)
协议开源。

- **开源与传染性**：你可以自由复制、修改和分发本项目代码。但如果你修改了代码并进行分发（无论是以源码还是二进制形式），你的衍生项目
  **必须**同样采用 GPL-3.0 协议开源。
- **保留署名**
  ：在分发或修改后的版本中，必须在显著位置保留原作者的版权声明及 [本项目](https://github.com/CookiesHax/CourseHelper)
  GitHub 仓库地址。

### 2. 免责声明

- **仅供学习**：本项目仅用于 Kotlin 开发技术交流及网络协议研究。
- **风险自担**：由于使用本工具可能导致的账号被封禁、学分异常或其他任何非预期后果，开发者不承担任何形式的法律及道德责任。
- **无担保**：本项目按“原样”提供，在法律允许的范围内，不提供任何形式的明示或暗示担保（包括但不限于对适销性或特定用途适用性的担保）。具体条款请参阅
  GPL-3.0 协议中的相关章节。
- **非分发**：本项目不提供任何编译后的二进制文件（Release），仅供开发者自行编译研究。

### 3. 尊重原创

本项目参考了 [AneryCoft/course_helper](https://github.com/AneryCoft/course_helper)
的部分逻辑，基于对开源精神的尊重，请在二次开发时同样保持对相关开发者的致谢。

### 4. 第三方 SDK 与依赖许可证

本项目在开发过程中使用了第三方 SDK 及开源库，其版权及许可证归原作者所有：

- 百度地图/定位 SDK：本项目集成了百度地图相关服务。该 SDK
  的使用须遵循 [百度地图开放平台](https://lbsyun.baidu.com/)服务条款。开发者在自行编译和分发时，需自行承担因违反其服务条款而产生的法律风险。
