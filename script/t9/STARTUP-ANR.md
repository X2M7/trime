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
