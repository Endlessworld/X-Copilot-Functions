# x-copilot-functions
### IDEA编程助手插件 ChatGPT X-Copilot Context Aware & Function calling 的本地回调函数代码库
#### 插件将在启动时初始化本仓库所有.groovy代码。所有groovy代码将首先被编译为Java class
#### 然后以每个方法为单位 根据注解信息被解析为 [ChatGPT function-calling参数](https://platform.openai.com/docs/guides/gpt/function-calling)
#### 然这些参数将被附加到每个请求中。ChatGPT将在恰当的时机在用户本地的IDEA平台中执行这些代码函数
#### 并将执行结果附加到当前对话的上下文信息中再次发送到ChatGPT。
#### 通过本代码库的一些简短的groovy代码片段（通常在数十行以内）
#### 可以使ChatGPT通过用户的本地主机 立即链接到到任何互联网服务、扩展任意功能和能力、从而释放AI大模型的无限可能性
