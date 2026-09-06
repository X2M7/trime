# 启动与主题部署阻塞回归

本轮版本为 `3.3.13-t9.2` / `20261103`。`VALIDATION.zh-CN.md` 保留上一个测试包的历史结果，不代表本轮验收结论。

## 修复边界

- `ThemeManager.activeTheme` 只读取已准备好的主题，不再触发同步部署。初始化与切换用协程互斥，文件解析在 IO 线程，主题发布与视图回调在主线程。
- `ThemeLoader` 经 `RimeApi.deployConfigFile` 调度 native 部署，不直接从 UI 线程调用 JNI。部署操作与完整维护任务互斥。
- 输入法服务初始化主题时可挂起，首次创建输入视图允许显示加载状态；完成后恢复实际键盘。失败可重试，销毁服务时取消等待。
- 移除输入法服务的重复 `DataManager.sync()`，由 Rime 启动线程统一同步资源。初始空方案对象不打开 native 配置文件。
- native 启动在线程内等待维护结束，再建立会话、发布状态和宣布 READY，避免首次输入使用尚未完成部署的方案。
- 绘制按键和开关菜单仅读取线程安全的运行时选项缓存。引擎通知和状态更新负责刷新，不在主线程等待引擎队列。

上述修改不清空用户词库，也不自动覆盖用户自定义主题/方案。需要重新编译 native 库，不能沿用 `t9.1` 的 `librime_jni.so`。

## 自动检查

单元测试：

```sh
./gradlew --no-daemon --max-workers=1 \
  '-Dorg.gradle.jvmargs=-Xmx1536m -XX:MaxMetaspaceSize=512m' \
  -Pkotlin.compiler.execution.strategy=in-process :app:testDebugUnitTest
```

模拟器集成探针是独立测试 APK 中的 `StartupResponsivenessInstrumentation`，不进入发布应用。它只允许 `ranchu/goldfish` 模拟器；使用临时只读 AVD 的应用专属测试数据。

```sh
./gradlew --no-daemon --max-workers=1 \
  '-Dorg.gradle.jvmargs=-Xmx1536m -XX:MaxMetaspaceSize=512m' \
  -Pkotlin.compiler.execution.strategy=in-process \
  :app:assembleDebug :app:assembleDebugAndroidTest
adb install -r /path/to/main-debug.apk
adb install -r /path/to/androidTest.apk
adb shell am instrument -w -r \
  com.osfans.trime.debug.test/com.osfans.trime.StartupResponsivenessInstrumentation
```

探针为主线程投递 50 ms 定时回调，覆盖引擎启动、主题初始化/重载、后台方案查询和按键标签读取。整轮最多 5 分钟，超过 4 秒无主线程回调仍判定失败，输出分阶段进度、`passed`、`main_ticks`、`max_main_gap_ms` 和异常堆栈。必须检查 `passed=true`，不能只看 adb 退出码。
这些数值是阻塞回归证据，不是按键延迟、帧率或真机性能基准。启动探针会重启目标进程，不能在正常个人输入过程中运行。

## 实际界面检查

1. 首次主题准备期间聚焦输入框，检查主界面仍可响应，随后出现实际键盘。
2. 重启进程并重复打开键盘；保留用户词库，不用清数据掩盖问题。
3. 执行重新部署，期间操作界面，部署后切换九键/全拼及中英文模式，确认候选、按键标签和输入均恢复。
4. 输入 `64426`，分别锁定 `ni/mi`，确认汉字选择与拼音选择仍独立；回归中段编辑、解锁、撤销。
5. 校验新包身份后保存截图、UI XML 和 Android `am_anr/am_crash` 事件，按 APK/PID/时间区分旧版本事件。

## 修复包预检查

2026-09-06，工作区构建 `0076f44145d0da3f32b4126fde4ce5bf24bf7d04684867fb5130acd6940f849f` 已安装到临时只读 `hk_api35`：Android 15/API 35，2 GB、2 核，arm64 库经 x86_64 原生桥运行，720x1600 / 280 dpi。它内嵌的 Git SHA 仍是 T00 基线，最终可追溯提交构建另行记录。

- Android 单元测试 62 项通过；T00 工具单元测试 26 项通过。
- APK 提取库 `d0a44d7e4ac7c0656e633f786505905abfeacb813f8feb03e7f4410cbeb2dcf9` 的 185 项控制器检查及新进程 3 项学习检查通过，记录在 `build/t9/d79fa2dfd64f/`。
- 首次部署中，测试输入面板可以打开并切换 Text/Number，键盘保持加载状态，没有新增 Trime ANR。完整系统词典编译未等到结束，不计为完整冷部署通过。
- 后续复用 T00 中与当前源词典 SHA-256 相同的 `luna_pinyin/stroke` table/reverse 文件；未导入 userdb、旧 prism 或旧主题。新版索引、主题由应用重新部署。这不是冷词库排序或首次安装性能基准。
- 第一轮旧探针达到 180 秒总时限，失败发生在后台连续方案查询阶段。第二轮保留部署数据、重启进程后通过：`passed=true`，主线程回调 3403 次，最长间隔 411 ms（`build/anr/instrumentation-2.txt`）。最终探针将总上限调整为 300 秒，但主线程 4000 ms 失败门槛不变。
- 模拟器启动时存在 System UI、Google 服务和 Digital Wellbeing 的独立 ANR；在只读测试实例中停用 `com.google.android.gms` 与 `com.google.android.apps.wellbeing` 后复测。没有修改原 AVD，不把系统进程事件当作 Trime 事件。

原始日志和截图在本地 `build/anr/`；测试包身份在 `build/baseline/0076f44145d0-f570a6d94085/`。仅此预检查还不能代表真机、release 签名构建或长期稳定性。

## 固定提交构建

最终运行时代码提交为 `07717d0d31c8c05da6413a9f1282194b00f0f5bc`，从干净工作区重新构建，62 项 Android 单元测试再次通过。后续验证文档提交不改变运行时代码。

| 项目 | 值 |
| --- | --- |
| APK | `com.osfans.trime-3.3.13-t9.2-dev-arm64-v8a-debug.apk` |
| 包名 / 名称 | `com.osfans.trime.debug` / `Trime`，debug 签名测试包 |
| versionName / versionCode | `3.3.13-t9.2` / `20261103` |
| 内嵌 Git SHA | `07717d0d31c8c05da6413a9f1282194b00f0f5bc` |
| APK SHA-256 | `8398cf96f2db9f0b4abff4739501b0d051357cf8b3a5b2444e47d837d7090286` |
| 签名证书 SHA-256 | `c6fb875b1959f56fc6b3992d603b86e2dcd793677d32aba83687b34ef6937dd2` |
| ABI | `arm64-v8a` |
| 九键 schema SHA-256 | `9f04415df4bc0342dfa9a11212ef34d57445c60c6b234c9e73835c685308c41f` |
| 资源集合摘要 | `05df6f1e0cd576a4955b23452ef18b0bda60095341c3cd149f840dd0a39cc413` |
| 测试 APK SHA-256 | `a562b9324862e95cf50b2f73f13f7fba387560c9c24a8e3e462468339027770c` |

冻结目录：`build/baseline/8398cf96f2db-ceeb3eaf5bcd/`。APK 内的 native 库与上述 185+3 项检查所用库逐字节相同，不沿用旧 `t9.1` 的测试结论。

第一次最终包探针在 `emulator-5554` 上执行时，整个模拟器退出、ADB 返回 255；该轮没有测试结论，不计为通过。主机内核日志未记录 OOM，模拟器日志为关闭流程，未据此推断 Trime 的故障原因。随后另起 `emulator-5560` 只读实例复测，不复用已退出实例的瞬时状态。

新实例刚开机时，PID `2634` 的 instrumentation 启动被 Android 判为 ANR。堆栈停在 `ActivityThread.initInstrumentation -> DexFile.openDexFileNative -> DexFileVerifier`，尚未执行应用初始化或 Rime；`schedstat` 记录主线程 CPU 时间约 2.1 秒、排队约 28.8 秒。证据保存在 `build/anr/instrumentation-bootstrap-anr.txt`，这轮不计通过，也不能声称所有启动尝试均无 ANR。只读实例中另停用 Google Search、Android System Intelligence 和 Restore 后，保留相同 APK 重试；不修改 Android 超时阈值或应用运行时代码。

最终包重试通过：`emulator-5560`，PID `3042`，`passed=true`，主线程回调 3152 次，最长间隔 316 ms。引擎启动、主题重载及默认主题回退、方案与运行时选项缓存、连续后台查询与按键绘制均完成；原始输出为 `build/anr/instrumentation-final-5560-retry.txt`，冻结副本为 `startup-passed.txt`。

随后正常启动应用，PID `3132`，通过实际界面从全拼切到九键，输入 `64426` 后同时显示 `mi/ni` 拼音选项及汉字候选。点选 `mi` 后锁定首音节、保留 `426`，候选缩小到“米高”等；UI XML 中编辑器仍为 `Type text` 提示，没有提交拼音或汉字。此次打开界面还显示了开机时 PID `818` 的旧 System UI ANR 弹窗，时间为 08:41:21；选择等待后继续测试，不能将该弹窗记为新的 Trime ANR。

随后点击汉字候选“米高”，编辑器内容变为 `米高`，组合和拼音区清空，九键仍可继续使用。最终包冻结目录中保存了 `capture-3c37ad1194b0/screenshot.png`（拼音锁定）、`ui-mi-lock.xml`、`ui-mi-commit.xml`、`ui-mi-commit.png` 及运行时方案/词典摘要。截图采集前已校验安装包与冻结 APK 完全相同；14 项证据有身份 sidecar，文件摘要复核通过。`android-failure-events.txt` 保留上述两次启动 ANR，最终探针 PID `3042` 与界面测试 PID `3132` 在截至 08:54 的记录中没有新增 ANR 或崩溃。

本轮仅验证受测阻塞路径和功能回归。模拟器使用软件 GPU 和 ARM 原生桥，界面日志仍有掉帧；没有测量手机按键延迟，也没有完成最终 APK 的全新/固定学习词库两组性能比较。冻结报告中的 `cold` / `learned` 保持 `not_run`，不能用启动探针或界面截图替代这两组结果。未清空个人词库、未修改原 AVD，未发布 release 签名包或更新 GitHub latest。
