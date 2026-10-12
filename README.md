# AutoLedgerCN 本地通知记账

Kotlin Android 应用：把用户授权监听的交易通知解析成流水，使用本地规则分类，通过交易指纹与跨来源规则去重，模糊结果进入待确认列表。工程由历史源码恢复，UI角色图片已改为自行编写的几何账本矢量；候选尚未重新构建或实机验收。

## 已实现的代码

- NotificationListenerService 接收所选App的通知；金额/收入支出/商户解析、非交易通知过滤及诊断。
- 原通知指纹、交易指纹与跨来源模糊匹配；合并来源信息，冲突交给用户确认。
- Room实体/DAO/仓储持久化，Flow与ViewModel驱动Compose页面；DataStore保存配置。
- 手工记录、流水与统计、预算参数、CSV导入导出、监听应用管理和模拟通知。
- WorkManager周期任务当前只查询交易，不代表完整后台统计计算。

当前分类器是 `LocalRuleClassifier`；`AiClassifier` 是保留的接口，`NoopAiClassifier` 返回null，**未接入大模型/云AI**。没有Java后端、云账号或登录系统。

## 环境与运行

保留原配置：AGP 8.7.3、Kotlin 2.0.21、Gradle wrapper 9.3.0、JDK toolchain17、compile/target SDK35、min SDK26；Compose BOM2024.12.01、Room2.6.1。没有为了版本号美化而改动历史工具链。

完整打开、构建及设备检查见 [运行说明](docs/RUNNING.md)。本次整理只做静态检查，没有执行Gradle同步/构建/测试。两份历史Debug日志有成功记录，但不代表当前候选已经构建通过。

## 架构与隐私

[数据流与边界](docs/ARCHITECTURE.md)说明通知、去重、仓储和UI；[隐私与资源](docs/PRIVACY_AND_ASSETS.md)说明通知访问、备份排除和中性资源。

所有演示应使用“模拟通知”或虚构数据，不能把真实手机通知/账单/CSV/数据库提交Git。应用通过用户操作的文档URI导入导出CSV；导出文件离开应用沙盒后的保管由用户负责。

## 验证与来源

[VERIFICATION.md](VERIFICATION.md)列实际执行和未执行的检查；13个原JUnit测试文件保留，通知解析新增异常金额用例，尚未执行。金额仍使用Double，本批没有进行全工程货币类型重构；数据库迁移、重复交易边界及恢复行为要在设备上验证。

本工程来自用户保存的个人项目档案，开发曾使用AI辅助；根据项目拥有者的个人项目来源说明及最终公开授权整理发布；第三方材料与业务源码的许可边界见 [来源说明](THIRD_PARTY_NOTICES.md)。没有为业务源码新增MIT等许可证。历史主题枚举与资源标识保留以避免破坏偏好兼容，显示文字改为“清浅/暖色”，不代表第三方角色授权。

候选文件见 [允许清单](PUBLIC_FILES_MANIFEST.md)及[排除清单](EXCLUDED_FILES.md)。公开内容仅限清单中的工程文件，不含私人归档、账单或角色图片。当前版本尚未重新构建，首次设备验收须按运行说明进行。
