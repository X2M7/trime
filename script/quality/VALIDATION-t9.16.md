# v3.3.13-t9.16 验证范围

状态（2026-10-15，发布前冻结）：本报告记录最终候选 `cc2d6bd6` 的限定模拟器范围。发布须经完整记录复核、源码打包和附件校验；在线状态以本版本 Release 为准。历史失败原件保留，不以旧候选通过替代当前候选。

## 源码与构建

- APK 编译源码：`cc2d6bd6aa1c8a2c863e0579aa568ed0cb40e016`；发布文档提交：以 `v3.3.13-t9.16` 标签所指向的文档提交为准。
- 版本：`3.3.13-t9.16`，versionCode `20261121`；minSdk 21，ARM64／x86_64。正式包虽沿用 `com.osfans.trime.debug` 包名，实际 Manifest 为非 debuggable。
- `candidate-build3` 的 Debug 新执行 812 项应用 JVM 测试，84 份 XML，失败／错误／跳过均为 0。227 项宿主检查新执行通过。
- Debug／Release Lint 分 JVM 新执行通过且 0 问题；构建和两个正式 APK 签名检查无警告。证书与 9.15 一致，v1／v2 签名有效。
- 384 项原生检查为历史执行，31 项输入与当前源码一致后绑定复用；本候选未重跑。build-logic 的 17 项和 compatibility 的 6 项不计为本轮新执行。生产 JNI 任务为 UP-TO-DATE，不能写成四组原生库本轮全部重新编译。
- 构建使用 896 MiB 堆、640 MiB Metaspace、单 worker；构建与模拟器串行，保留 1 GiB 可用内存下限与文件索引进程。

| 正式 APK | 字节数 | SHA-256 |
|---|---:|---|
| ARM64 | 11,966,361 | `616f595686bed3269ef137bedb79e174a753c44385f47713d15982fbcaf14ccd` |
| x86_64 | 12,047,568 | `cc7d2601894b1f5d0d1cd77b01db83c305f0d3ce0affe0a8939fa40234f31ea3` |

## 模拟器限定范围

| 范围 | 当前结果与边界 |
|---|---|
| Android 5.0 runtime7 | 已正常关闭，控制器／VM／logcat 退出 0，无清理错误或本轮残留。最低可用内存 3,354,344 KiB。 |
| Android 5.0 四矩阵 | 竖屏、竖屏大字体、横屏、横屏大字体；两主题共 24 张原图已由 Root 复核。 |
| Android 15 runtime17 | 正常 STOP；控制器和 VM 实际退出 0，logcat 在设备关闭时退出 255，原件保留。控制器采样最低可用内存 1,539,788 KiB（约 1.47 GiB）；分屏子阶段记录到 1,489,296 KiB（约 1.42 GiB），保护未触发、无清理错误或残留。 |
| Android 15 八矩阵 | 手机两字体、横屏两字体、夜间＋手势导航两方向、平板两方向；48 张原图已复核。8 个原始 probe 退出 0、恢复配置；外层 tool wait handle 已不可用，不补造外层实际等待凭证。 |
| 真正深色补测 | Android 15 公开选择 Steam 配色，再跑深色／手势导航／横屏／大字体一例，六张原图已复核。八矩阵中的 night 配置 `followSystemDayNight=false` 且为浅色配色，不能作为深色像素验收。 |
| 两系统基础回归 | 设置 14 项及缓存相关检查、词库 14 项／200 候选、隐私 16 项／三方案、两主题 40 轮编辑器显示及 100 次显隐、联想 30 项、输入时序 8 项／320 FIFO／49 候选、反馈 FD 检查通过。API21 不支持敏感 ClipDescription 标记，不扩展为该平台已测。 |
| Android 5.0 正式版公开输入 | 两主题实际 `64→mi→米`、一次空格／删除／发送动作通过；这不是 API21 TalkBack 验收。 |
| Android 15 正式版 TalkBack | 实际 TalkBack FOSS 13，十个硬件手势、十一帧观察；探索不提交，双击筛选 `mi`，候选“米”只提交一次，发送动作读回 4。不是系统 TTS 朗读模拟，也不代替 Google Play 最新 TalkBack 或真机。 |
| Android 15 分屏 | 正式 APK 的字体 1／2、上下两个实际任务位置均通过：每组 20 个真实硬件手势，探索不提交，筛选 mi 后每位置只追加一次米，最终 T13_SPLIT米米；Root 查看 9 张关键原图，全部配置与服务恢复。解析与同步失败轮原件保留。 |
| Android 15 SAF | 实际 DocumentsUI 创建与授权目录、取消重选两次 Back，取消前后偏好及授权全字节一致；严格导入七项通过。导入阶段 `general__pid` 会变化，不能将导入前后整份偏好也写成字节一致。 |
| 词库升级 | 两系统均为受控 Debug 准备→旧正式 9.15→新正式 9.16，同 UID／证书／目录。两条虚构词学习与三份 262 字节备份一致，全拼／九键查词通过；不代表任意旧安装和任意真实用户数据。 |

两倍字体下，固定 48dp 的测试编辑框自身裁切字形顶部，测试按钮也有自身文字裁切；原图保留，不将其称为所有外部应用内容完整可见。候选／键盘区域与编辑器没有相交。短横屏与字体 2 布局通过垂直滚动访问内容，部分上方候选或底部按键不在同一视口，不能写成所有控件同时完整可见。平板结果是模拟器分辨率配置，不能替代物理平板。

## 警告与未验收范围

Android 5.0 当前完整保留日志有 2,234 W、1,631 E，共 3,865 条；既有 fatal／LMK／内核 critical 模式为 0。项目 ThemeUtils 错误本轮未复现。40 条 SQLite code 283 是 `NOTICE_RECOVER_WAL`，旧 Android JNI 将这类通知映射为 E；原始级别保留，不称数据库损坏，也不推断完整数据库健康检查通过。AudioTrack FAST、旧 WebView、EGL 等记录按类别保留。

Android 15 runtime17 的完整留存日志有 19,802 W、7,313 E，共 27,115 条；项目归属为 811 W、101 E。全 PID 的 ThemeUtils、PhoneWindow 焦点警告、Ripple 软件 Canvas 警告、WrappedRunnable 超阈值等待与旧 Session 异常本轮均未复现；这只是当前实测范围。既有 critical／LMK／内核 critical 模式为 0，但仍有一条真实 WebView renderer `crash detected (code -1)`：原日志 L65199–65201 中，remove／destroy 用时 6.863726 ms，结束后 8 ms 出现错误，随后系统回收 renderer。不能将相关时序说成唯一原因已修复或无错误退出。

其余保留记录包括项目 FrameTracker 61 E／163 W、Binder 29 E，系统 AtomicFile rename 27 E、audit 丢失／限流 10 条。InputManager 的 249 W 全在系统目录 UI20 的一次 `input swipe` 区间，早于 TalkBack 启动；该归因是时序关联，未单独捕获 injector 执行文件身份，命令退出 0 不证明该次滑动全部成功，后续实际目录授权／取消以独立截图、字节及授权证据为准。另有 61 条无障碍窗口等待超时及 1 条 SplitCompat 错误保留，不能据特定通过帧断言完整无障碍系统正常。TalkBack FOSS 的 249 E 为 TTS 尚未初始化，与前述 249 W 并非同一组事件；正式版探索／双击的内核触摸及编辑器结果通过，不表示语音朗读通过。完整日志在正常关机时的 logcat 退出 255、早期启动未完整覆盖与 audit 丢失均限制日志完备性。

本报告不宣称所有系统警告、所有 bug 或所有风险已消除。实物 ARM64／OEM／16 KiB 页、真实语音和 TTS、候选 P95／Java-native-graphics PSS、长期驻留与全部第三方主题／插件均未全面验收。

## 证据与发布

当前候选完整构建清单、源码、签名及两系统原件位于 `build/t13/candidate-build3`、`candidate-artifacts3-inspection1`、`api21/runtime7`、`api35/runtime17` 及其 `candidate-*` 子记录。最终限定范围复核与绑定见 `build/t13/root-final-scope-reviews1/`；发布资格及实际操作另由冻结的 qualification 和发布回执记录。

发布流程在本报告冻结后核对 docs-only PUB、冻结资格记录、完整源码包逐 Git 文件内容与离线发布预检。实际发布后以回执核对 fork develop／tag、Latest 及三个公开附件 SHA-256。手动附件仅两个 Release APK 和完整源码包，不上传测试报告、签名文件或调试产物。
