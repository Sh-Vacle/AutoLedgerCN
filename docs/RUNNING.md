# 用户外部构建与设备验收

以下步骤留给项目拥有者在普通PowerShell/Android Studio执行；本次整理没有执行它们。

1. 安装/选择JDK17和Android SDK35。将本机 `JAVA_HOME` 指向JDK17，确保SDK可用。
2. 在工程根目录创建本机 `local.properties`，内容为 `sdk.dir=<本机SDK路径>`，不要提交此文件。
3. 在普通PowerShell中进入工程根目录，确认 `java -version` 与SDK路径。
4. 运行 `./gradlew.bat :app:assembleDebug`；网络下载、环境兼容或编译错误保留完整私有日志。
5. 另运行 `./gradlew.bat :app:testDebugUnitTest`。当前保留的JUnit源码没有本次通过记录。
6. APK安装后先使用模拟通知；确认首页/流水/统计/待确认/预算/CSV导入导出及两套配色。
7. 自行授权通知读取后，用虚构交易检查监听、重复通知、跨来源合并和同额不同商户；不要自动把真实数据截图上传。
8. 检查通知撤权、重启、数据库升级和云备份/设备迁移行为。没有板端、生产环境或商店发布验收。

minSdk26（Android8）；Android13+需要通知权限。QUERY_ALL_PACKAGES只供现有已安装App管理，是否可进入应用商店需要另行核对政策，本批没有向商店发布。

工具链保留历史Gradle9.3/AGP8.7.3组合；历史日志显示Debug成功，不保证当前SDK/JDK环境复现。出现版本问题先看真实日志，不预先随意升级/降级整套依赖。
