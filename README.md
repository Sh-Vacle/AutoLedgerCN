<p align="center"><a href="https://github.com/Sh-Vacle/AutoLedgerCN"><img src="assets/logo.svg" width="94" alt="💰 AutoLedgerCN Logo"></a></p>

<h1 align="center">💰 AutoLedgerCN</h1>

<p align="center">授权通知解析、交易去重与本地账本</p>

<p align="center"><img src="assets/badges/tech-0.svg" alt="Android API26+"> <img src="assets/badges/tech-1.svg" alt="Kotlin 2.0.21"> <img src="assets/badges/tech-2.svg" alt="Compose BOM2024.12.01"> <img src="assets/badges/tech-3.svg" alt="Room 2.6.1"></p>

读取用户授权的交易通知，解析金额、收支和商户，通过指纹与跨来源匹配处理重复或冲突，再保存到本地 Room 数据库。界面使用 Jetpack Compose，偏好设置使用 DataStore。

[业务源码](app/src/main/java/com/autoledgercn) · [运行说明](docs/RUNNING.md) · [架构](docs/ARCHITECTURE.md) · [验证记录](VERIFICATION.md)

## ✨ 功能与工程难点

- **通知接入**：NotificationListenerService 监听用户选择的应用，过滤非交易候选并保存诊断。
- **金额解析**：提取金额、收支、商户、账户和时间；拒绝非有限或非正金额。隐藏金额、歧义通知进入待确认流程。
- **交易去重**：原通知指纹、5分钟交易指纹和10分钟跨来源规则分别处理重复通知与多来源交易；冲突交由用户确认。
- **持久化与界面**：Room保存流水、原通知、分类规则、待确认项和诊断；Flow与ViewModel驱动Compose页面，DataStore保存偏好。
- **账本操作**：手工记录、流水查询、统计页面、预算参数、CSV导入导出、监听应用管理与模拟通知。
- **后台任务**：WorkManager的StatsRefreshWorker目前只查询交易，不执行完整后台统计计算。

通知格式与商户描述会变化，不能只靠金额判断同一笔交易。金额仍使用Double，舍入、并发去重和Room迁移是设备验收重点；不能保证所有银行、电商通知都被正确识别。

## 🔁 通知到账本的数据流

<p align="center"><img src="assets/pipeline.svg" width="420" alt="AutoLedgerCN业务流程图"></p>

图中包含待确认路径，并非所有通知都会直接入账。实现入口：[通知服务](app/src/main/java/com/autoledgercn/service/AutoLedgerNotificationListener.kt)、[解析器](app/src/main/java/com/autoledgercn/domain/parser/NotificationParser.kt)、[去重引擎](app/src/main/java/com/autoledgercn/domain/dedup/DeduplicationEngine.kt)、[仓储](app/src/main/java/com/autoledgercn/data/repository/LedgerRepository.kt)。

## 🚀 本地运行

工具链：JDK17、SDK35、minSdk26、AGP8.7.3、Kotlin2.0.21、Gradle wrapper9.3.0。配置本机 `local.properties` 后，按[运行说明](docs/RUNNING.md)在普通PowerShell或Android Studio构建、测试和安装。当前工具链兼容性以实际构建日志为准。

```text
app/src/main/java/com/autoledgercn/
├── service/       # 通知监听、后台任务
├── domain/        # 解析、去重、分类
├── data/          # Room、仓储、偏好
└── ui/            # Compose、ViewModel
app/src/main/res/  # Manifest引用的实际资源
app/src/test/      # 本地JUnit源码
```

## 🛠️ 项目状态

当前版本完成了工程结构、资源引用和源码差异的静态检查，**尚未构建或实机验收**。13个JUnit测试文件及新增异常金额用例未运行；历史Debug成功记录不能替代本版本验收。暂无可验证的App截图。

分类由 `LocalRuleClassifier` 完成；`AiClassifier` 仅为接口，`NoopAiClassifier` 返回null，未接入大模型或云AI。应用没有后端服务或登录系统。

## 🔒 隐私与来源

应用没有INTERNET权限，但通知正文和账单仍是敏感数据。演示使用模拟通知或虚构交易；CSV导出后离开应用沙盒，需自行保管。已禁用备份并配置排除规则，OEM云备份与设备迁移尚未实测，见[隐私说明](docs/PRIVACY_AND_ASSETS.md)。

原角色素材未纳入仓库，当前使用几何矢量；历史主题标识保留以兼容已有偏好。业务源码尚未指定LICENSE，借鉴内容的再许可权仍需确认；Gradle wrapper独立保留Apache-2.0。开发与调试使用过AI辅助，详见[第三方材料](THIRD_PARTY_NOTICES.md)和[许可核查](docs/LICENSE_REVIEW.md)。

[公开文件清单](PUBLIC_FILES_MANIFEST.md) · [排除清单](EXCLUDED_FILES.md)
