# t9.3 上游合并验收

本轮从 fork 的 `631798c18f63b6cb409113eca8f796ab409fb778` 开始，合并 `osfans/trime` 的 develop 至 `d9a1f424c63d7de34dce453e9ca8214cf7ae287e`，共补入 14 个提交、78 个文件的上游改动。上游 main 也已包含在这个 develop 提交中。
版本递增为 `3.3.13-t9.3` / `20261104`，保留九键、独立拼音消歧和异步主题部署；不是用上游文件覆盖整个 fork。不向 `osfans/trime` 推送，不变更 Release/latest。

## 纳入的改进

- 颜色键类型化和预编译颜色表；GeneralStyle 默认值集中定义。
- ThemeScope 注入输入界面，切换配色原地刷新；候选、弹窗、剪贴板、符号和其他面板一并更新。
- 按 installation_id 区分本机/其他设备的同步目录，读取 installation.yaml 中的 sync_dir。
- 导入不覆盖本机 installation.yaml 和本机同步导出目录，导出仅处理本机 ID；支持自定义同步目录。
- 修正 lint 报告任务对资源摘要的依赖及 OpenCC 安装任务的弃用调用，补齐繁体翻译。

## 合并适配与修复

- ThemeManager 保留挂起加载和互斥，适配上游 ThemeScope；结构相同的主题重载保留同一个 scope，避免旧视图与全局配色脱节。
- 输入服务保留加载界面和生命周期检查，导航栏、自动填充建议在主题未就绪时不读取未初始化的颜色。
- 九键拼音区域使用注入的 scope，切换配色更新拼音、选中段、图标及背景，保留锁定输入与动作 revision；候选控件重新绑定时重读文字颜色，避免 RecyclerView 复用旧配色。
- 部署后的主题重载异常被记录而非从生命周期协程传播为崩溃；取消仍正常传播，首次部署由初始化流程处理。
- 同步 ID 必须是单个有效目录名；拒绝路径分隔符、点路径和控制字符，校验本机同步目录不能经符号链接越界。
- 安装 ID 缺失或无效时保留已配置同步根目录中的备份，但仍允许导入其他设备的同步文件；不以身份缺失为由清空备份。

## 固定构建

测试日期：2026-09-06。正式合并提交是 `8b3b04d0a295493442a3367e16f4b51f8ed80bec`，两个父提交分别是上述 fork 与上游提交。APK 从该提交的干净工作树生成，后续仅补充验收文档。

| 项目 | 值 |
| --- | --- |
| 包名 | `com.osfans.trime.debug` |
| 显示名称 / 构建类型 | Trime / Debug，非正式发布签名 |
| versionName / versionCode | `3.3.13-t9.3` / `20261104` |
| APK 内嵌 Git SHA | `8b3b04d0a295493442a3367e16f4b51f8ed80bec` |
| APK SHA-256 | `5cd9fb3c43a0a8e8e5e113dec26561b013fa8100f104b5641b3de3f0b953f1ef` |
| 签名证书 SHA-256 | `c6fb875b1959f56fc6b3992d603b86e2dcd793677d32aba83687b34ef6937dd2` |
| APK ABI | `arm64-v8a` |
| 测试设备 | `hk_api35`，Android 15 / API 35，x86_64 模拟器转译 ARM64 |
| 九键 schema SHA-256 | `9f04415df4bc0342dfa9a11212ef34d57445c60c6b234c9e73835c685308c41f` |
| 打包资源摘要 | `05df6f1e0cd576a4955b23452ef18b0bda60095341c3cd149f840dd0a39cc413` |
| 原生库 SHA-256 | `d0a44d7e4ac7c0656e633f786505905abfeacb813f8feb03e7f4410cbeb2dcf9` |
| T00 身份记录 | `build/baseline/5cd9fb3c43a0-f72ab615da08/` |

本地 APK：`app/build/outputs/apk/debug/com.osfans.trime-3.3.13-t9.3-dev-arm64-v8a-debug.apk`。先用 T00 prepare 固定 APK，再于安装后、界面回归后两次 capture 核验安装包身份；截图与日志附带身份 sidecar，归档后 63 项 artifact 摘要验证通过。日志和截图保留在本地构建目录，不随源码提交。

## 已执行回归

- `testDebugUnitTest`：205 项，0 失败、0 跳过；包括颜色表、ThemeScope、方案缓存、同步路径、缺失 ID 的备份保留及符号链接越界测试。
- 基线工具自测：26 项通过。
- `assembleDebug`、`assembleDebugAndroidTest` 及本轮手改 Kotlin 的格式检查通过。
- 独立执行 `lintDebug`：0 错误、83 警告；没有新增全局抑制或关闭检查。原始报告见 `build/upstream-merge/lint-results-debug.{xml,html}`。
- 新 APK 提取的 ARM64 库重新运行 185 项九键控制器检查，另起进程运行 3 项学习持久化检查，全部通过；涵盖分段锁定、中间编辑、撤销、补全、分词歧义、多音字和全拼/双拼。原生驱动记录在 `build/t9/58b0e15b0c28/`，使用小型测试词典，不代表正式词典排序。
- `StartupResponsivenessInstrumentation`：`passed=true`、结束码 `-1`，主线程心跳 3305 次、最大间隔 596 ms，低于该探针的 4000 ms 阈值。覆盖主题未就绪时自动填充请求、相同主题 scope 保持、主题回退、方案/选项缓存、繁忙引擎查询期间绘制。
- 同一 instrumentation 在真实 Android View 上验证横竖屏配色切换：拼音、选中段和图标颜色更新；布局高度、原始编码、锁定、revision 不变；刷新不发送输入动作，刷新后的段点击仍携带正确 revision。
- 真实输入界面：全拼 `nihao` 可选择“你好”；九键 `64426` 同时列出 `mi/ni`，选 `mi` 显示“米高”等词并保留 `426`；撤销后可改选 `ni`，仍保留 `426`。两次拼音锁定后编辑器仍为空，点击汉字“你好”才提交。XML 断言与截图均保存。

关键界面证据为 `build/upstream-merge/ui-full-pinyin-ready.png`、`ui-full-commit.xml`、`ui-t9-64426.png`、`ui-t9-mi.png`、`ui-t9-undo.png`、`ui-t9-ni.png`、`ui-t9-commit.{png,xml}`。空编辑器在 UIAutomator 中显示占位提示 `Type text`，不是已提交字符串。
完整关联归档位于 T00 记录的 `merge-regression/`，汇总为 `verification.json`。

## 环境与边界

首次将编译与 lint 连续放在一个 Gradle 进程中遇到 512 MiB Metaspace 上限，失败记录保留在 `build-preflight.log`；拆成独立进程后 lint 和最终构建均成功。构建始终使用一个 worker、1536 MiB 堆、512 MiB Metaspace，不与模拟器同时运行。

本轮 JNI、Rime/OpenCC 子模块和打包词典未改变，最终 APK 与 t9.2 的原生库及全部内置资源逐文件摘要一致。构建临时复用该原生库，不复用旧 APK 的 Kotlin 代码；验证后已删除本轮创建的 `app/prebuilt/`，避免未来原生源码修改被缓存遮蔽。

模拟器使用 `-read-only -no-snapshot -memory 2048 -cores 2`，已关闭。仅在临时测试实例关闭部分 Google 后台应用；只预置 t9.2 冷词库基线中相同资源生成的四个系统 table/reverse 文件，没有导入用户词库。主题与 prism 由新 APK 部署。
本 APK 的 T00 cold/learned 两组性能基线仍为 `not_run`，不能把本轮 UI 验证写成完全冷部署或固定学习后的性能结果。历史 t9.2 两组结果保持独立。

系统事件记录一次 `com.android.systemui` 启动 ANR（17:11:00，早于 Trime 测试进程），保留了提示截图与事件日志；未观测到本轮 Trime 崩溃或 ANR。模拟器转译与受限资源下的响应时间不能代表手机性能。

83 条 Lint 警告仍需分类处理，包括依赖更新、未使用资源、程序化 View 构造函数提示，以及静态 Keyboard 持有 Context 的生命周期风险等；本轮没有扩大为键盘生命周期或崩溃处理机制重构。真机厂商差异、多 Android 版本、长时间使用、不同 SAF 文件提供器、正式签名包仍待单独回归。
此次结论是本次合并与上述回归通过，不是“所有 Bug 已消除”，也不更新 Release/latest。
