# AI 小说转剧本工具

一个面向小说作者的 AI 辅助改编工具。用户输入 3 个章节以上的小说文本后，系统会将原文拆解为章节、场景、角色、动作和对白，并输出可继续编辑的结构化剧本 YAML。

### 演示视频
百度网盘：https://pan.baidu.com/s/1-RtI20k94onqCwG61b2Flg?pwd=n6wu

## 功能

- 输入小说标题、原文来源和不少于 3 个章节的小说正文
- 支持用户注册、登录和退出，登录后才能发起剧本转换
- 支持普通用户 / VIP 用户身份，用户可充值账户余额，开通 VIP 会扣除 10 元
- 支持上传 `.txt` / `.md` 小说文本文件并自动填充正文
- 支持保存、查看、载入和删除转换历史记录
- 支持普通用户每日 3 次转换限制，VIP 用户不限制转换次数
- 支持生成后 YAML 格式校验，并提供 Schema 示例查看与复制
- 自动生成符合 Schema 的剧本 YAML
- 前端页面展示转换结果，支持复制和下载 `.yaml`
- 前端支持一键载入测试样例、章节数统计和字数统计
- 未配置 API Key 时会根据章节标题生成演示 YAML，方便无密钥环境下展示项目流程
- 后端对章节数量做基础校验，避免输入过短导致改编结果不可用

## 技术栈

- Java 17
- Spring Boot 3.2
- 原生 HTML/CSS/JavaScript
- MySQL + Spring JDBC
- OpenAI 兼容 Chat Completions API（默认通义千问 DashScope）

## 环境要求

- JDK 17
- Maven 3.8+
- Git
- MySQL 8.x

## 运行方式

1. 设置模型平台 API Key。

方式一：复制 `.env.example` 为 `.env`，然后把 `DASHSCOPE_API_KEY` 改成自己的通义千问 DashScope Key。VS Code 的 `Run Spring Boot App` 会自动读取 `.env`。

```text
DASHSCOPE_API_KEY=your_dashscope_api_key_here
LLM_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
LLM_MODEL=qwen-turbo
MYSQL_URL=jdbc:mysql://localhost:3306/novel2script?createDatabaseIfNotExist=true&useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
MYSQL_USERNAME=root
MYSQL_PASSWORD=your_mysql_password_here
```

方式二：在当前 PowerShell 临时设置环境变量。

```powershell
$env:DASHSCOPE_API_KEY = "your_dashscope_api_key"
```

可选：指定模型。

```powershell
$env:LLM_MODEL = "qwen-turbo"
```

如果要切换到其他 OpenAI 兼容平台，只需要修改：

```text
LLM_BASE_URL=平台提供的兼容接口地址
LLM_MODEL=平台模型名称
LLM_API_KEY=平台 API Key
```

MySQL 会在应用启动时自动创建 `users` 表；如果连接用户有建库权限，`createDatabaseIfNotExist=true` 会自动创建 `novel2script` 数据库。

2. 编译并运行：

```powershell
mvn clean package
mvn spring-boot:run
```

3. 打开浏览器访问：

```text
http://localhost:8080
```

## 测试

运行单元测试：

```powershell
mvn test
```

当前测试覆盖：

- 中文章节标题识别
- 章节标题提取
- 未配置 API Key 时的多章节演示 YAML 输出
- 用户注册、重复用户名、登录密码校验逻辑

测试中使用 mock 的 `JdbcTemplate` 验证用户逻辑，不再引入 H2 内存数据库。

## VS Code 运行

1. 安装推荐扩展：打开项目后，VS Code 会根据 `.vscode/extensions.json` 提示安装 Java 和 Spring Boot 扩展。
2. 确认本机已安装 JDK 17，并且 `java` 命令可以在终端中运行。
3. 复制 `.env.example` 为 `.env`，填入自己的 `DASHSCOPE_API_KEY`。
4. 打开 `src/main/java/com/example/novel2script/Application.java`。
5. 点击编辑器右上角的运行按钮，或在 Run and Debug 面板选择 `Run Spring Boot App`。
6. 启动后访问 `http://localhost:8080`。

## API

`POST /api/convert`

请求示例：

```json
{
  "title": "远方的灯火",
  "source": "前三章",
  "novelText": "第一章...\n第二章...\n第三章..."
}
```

响应示例：

```json
{
  "yaml": "title: \"远方的灯火\"\nchapters:\n  - chapter: \"第一章\"",
  "error": null
}
```

## 目录说明

- `src/main/java`：Java 后端实现
- `src/main/resources/static/index.html`：前端页面
- `src/main/resources/application.properties`：MySQL 连接配置
- `YAML_SCHEMA.md`：剧本 YAML Schema 说明与设计原因
- `schema.md`：Schema 简版说明
- `examples/input_novel.md`：示例小说输入
- `examples/output_screenplay.yaml`：示例剧本输出

## 提交说明

当前项目适合作为题目三“AI 小说转剧本工具”的 MVP。核心链路完整，后续可以继续增加 YAML 校验、分章编辑、单场景重生成和导出 PDF 等能力。
