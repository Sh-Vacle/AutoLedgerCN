# 数据流与实现边界

```text
NotificationListenerService
  -> NotificationCandidateFilter / NotificationParser
  -> raw fingerprint + transaction fingerprint + DeduplicationEngine
  -> LedgerRepository -> Room LedgerDao / entities
  -> Flow -> LedgerViewModel -> Compose UI
```

解析器提取金额、类型、商户、账户和时间；规则分类器按关键词/用户规则分类。原通知完全去重、5分钟交易指纹和10分钟跨来源候选规则是不同层次；不能保证所有银行/电商通知都能被可靠匹配。隐藏金额或歧义通知进入待确认流程。

Room保存流水、原通知、待确认、分类规则及诊断；DataStore保存监听应用、主题、预算等偏好。原通知的正文可能含敏感信息。Activity中还有较集中的UI逻辑，未做大型架构重写。

金额解析与CSV导入现在拒绝非有限/非正数；对已有正常正金额通知与去重策略不作重写。Double金额舍入、通知并发去重与数据库迁移仍是设备测试重点。版本2的MIGRATION_1_2和13份原测试保留；没有原版本1 schema，不能证明所有升级路径已覆盖。

`NoopAiClassifier`不提供分类能力；WorkManager的StatsRefreshWorker只读交易列表。视频/后端/AI训练均不属于此工程。
