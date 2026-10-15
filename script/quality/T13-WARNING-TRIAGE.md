# T13 运行警告分诊

状态：本地代码候选，尚未推送或发布。最新API21 runtime6保留2177W／1320E／0F、项目149W／92E；本轮ThemeUtils、旧Session fatal、am_crash／ANR、真实LMK和内核critical未匹配，仅是check30 Debug的有限观察，旧fatal及系统警告均保留。11个已闭合probe阶段各有原native true／完成码−1和宿主0，Root正常STOP后controller／VM0；不代表新候选或警告全部消除。10处配色和3.3.13-t9.16／20261121身份已写入，未构建；当前独立Lint／Release及最终两平台仍待验。

基线是 `d6199c86e7ad358158eec105d8ec46e1b27cdeff` / Build25：API21 runtime27 捕获 4,233 条 W/E/F，API35 runtime11 捕获 16,591 条。两轮 critical、LMK、kernel critical 均为 0，但 `raw_warning_free=false`。Root 原批准只覆盖实测 T12 功能，不表示系统持久化、资源释放或性能问题已经修复。

原批准：[API21](../../build/t12/api21/runtime27-root-warning-adjudication1.json)、[API35](../../build/t12/api35/runtime11-root-warning-adjudication1.json)。有限原因证据：[API21 异常](../../build/t12/api21-runtime27-finite-warning-exceptions1/report.json)、[API35 项目原因](../../build/t12/api35-runtime11-project-warning-cause-advisory1/exact-lines-source-and-findings.json)。下表保留 T12 当时的分类与调查方向，行号指这两轮各自的 `logcat-full.txt`；新增实测结果见下方 runtime2 小节。

## 分类与下一步

| 归属 | 已观察的问题与证据 | 可执行修复／验证 |
| --- | --- | --- |
| Trime 可修，需定位阶段 | API35 L53814，当前 Debug 导入 PID30093 的 `WrappedRunnable` 等待 **3305ms**。不是测试注入延迟，也不是候选 P95。`RimeDispatcher.kt` 的 2000ms 警告阈值仍然有效。 | 对队列任务名称、native 部署、主题编译／解码、事务提交分别计时。`activateImportedNativeConfined()` 同步 `startRime(true)` 并解码主题；先定位实耗，再减少重复部署或无关主题加载。SAF staging 在 T12 已与 native activation 分开，不能把整段移到 IO 后宣称问题解决。维持单线程 JNI、输入顺序、事务与取消／回滚边界；不提高阈值、不丢输入。 |
| Trime 可修，当前源码修复待测 | API21 L15662、API35 L52739 的 PhoneWindow 恢复焦点警告都指向 ID2131362349（Build25 符号表中的 `trial_input`）。`FragmentStateAdapter` 页面视图晚于 Window 的即时焦点恢复附着。 | 由 Setup 明确保留试打焦点意图；保存 Window 层级时使用已存在的 pager 作为稳定焦点持有者，并在同一主线程调用内恢复当前编辑器。重建后仅在 Try 当前、视图已布局、Activity RESUMED 且拥有窗口焦点时恢复。新增实际 probe 检查编辑器 token、IME 可见性、草稿、selection、composing span、checkpoint 均不变；现有真实 recreation 再检查试打焦点。不得在 Advanced／权限窗口仍拥有焦点时抢回，不修改私有 Window Bundle 键。 |
| 应用触发的框架兼容问题 | API35 L52759 当前 Setup PID29903 的 ImeBackDispatcher 注销回调未找到；L61666–61672 当前 PID30215 的 RippleDrawable 在软件 Canvas 上跳过 patterned 动画。 | 结合实际 editor/window 生命周期检查重复注销、异步旧 callback 与绘制路径。软件 Canvas 路径应使用受支持的按压反馈，避免只改日志级别；保留硬件绘制效果及触摸反馈。当前定位不能证明所有 callback 或图形异常均由应用造成。 |
| 验收环境可调查／修复，非 Trime 文件 | API35 **4** 次 AtomicFile rename 失败：L23434、35857、36098、50723，均为 system_server PID588 的 `/data/system/install_sessions.xml.new` → `install_sessions.xml`。日志未提供具体 errno。 | 在全新隔离 userdata 副本里检查 `/data/system` 的所有者、权限、SELinux context、可用空间／inode、安装 session 写入与安装时间关系，再用干净基线对照。只有证实原因后修复该副本并重跑安装／升级；不要改 Trime 的词库 AtomicFile 来处理这些记录，也不要删／chmod 旧系统原件。安装成功不等于 session 持久化健康。 |
| 验收环境可调查／修复 | API21 L2835–2846 的 **6** 条 `log_files.xml` 目录创建失败来自 system_server1496 / BootReceiver 复制已有 tombstone；旧 observer／Trime APK 路径 zipro 报错早于当前 APK 安装。 | 保留旧 userdata、tombstone 和 APK 路径证据；另建干净可写 userdata 对照，核对系统目录与包管理记录。不能只清空旧日志或把旧 APK 错误归到当前已认证 APK。API21 修复 SD 副本的证据也不能证明整个 guest `/data` 健康。 |
| 无法通过 Trime 释放的其他应用／系统资源 | API21 **43** 条 StrictMode 是 **3** 个真实栈：MMS2101 的 `DrmManagerClient.release`（L5421–5431），Calendar2128 的 `CursorLoader`／Cursor close（L5432–5446，另 L5447 finalization），LatinIME1706 的 `InputEventReceiver.dispose`（L10032–10048，另 L10031 InputChannel finalization）。 | 修复或升级对应系统组件；若做环境对照，使用独立镜像并保留原互操作测试。Trime 不能 close／dispose 其他进程的对象，也不能通过关闭 StrictMode 掩盖泄漏。LatinIME callback timeout 的远端 IInputContext 身份仍未证实；不能据此断言 Trime 卡死或系统无害。 |
| 无法直接通过 Trime 修复的系统实现 | API35 DocumentsUI1992 的 StrictMode 非 UI Context inflater 栈（L23088 起），系统 DocumentsUI Back 诊断；其他 optional HAL、memtrack、系统图形／服务、audit loss 与 overflow 记录。 | 使用对应修复后的系统镜像／组件对照，必要时向平台提交原始栈。Trime 自己的 View 仍应使用 Activity／IME UI context；此轮 DocumentsUI 栈不是本项目 inflater。缺少 memtrack 限制 Java/native/graphics PSS 归因；audit loss／event overflow 限制“所有底层事件均被观察”的结论。保留这些剩余风险。 |
| 测试 WebView 清理相关，原因未闭合 | API35 L75276，当前 DebugEditor PID30451 报 Renderer30544 `crash detected (code -1)`；随后 system_server 以 `isolated not needed` 清理该 renderer。源码在 WebView 邮箱输入验证后返回 chat、remove／destroy WebView。时序相关尚不能证明这是正常关闭或无损退出。 | 已在测试 remove／destroy 两侧加入同 run、owner PID、WebView identity 的 `WEBVIEW_CLEANUP` start/end 原始日志，使用 `SystemClock.elapsedRealtimeNanos()` 单调时钟及实际 duration。下一轮对照 renderer 退出与这对标记；不新增吞异常的 `onRenderProcessGone`，不隐藏 Chromium 错误，不以原全轮功能通过代替 renderer 原因验证。 |

API35 的旧 6 条 Ripple 错误位于 DebugT10 的候选离屏布局阶段。`PersonalDictionaryProbe` 调用 `CompactCandidateLayoutAssertions`，其中使用真实生产 View／adapter 向 `Canvas(Bitmap)` 绘制。T13 runtime1 实测说明仅调用 `jumpDrawablesToCurrentState()` 不足以解决问题：词库检查通过，但仍出现 2 条软件 Canvas 错误。后续测试夹具在 API29+ 使用公开的 `RenderNode.beginRecording()` 硬件 Canvas，API21–28 保留 Bitmap 路径；布局、触摸、身份与提交断言不变。runtime2 的新 APK 已通过 T10 的 14 项检查且对应进程未出现该错误，范围见下文；离屏绘制记录不是实际屏幕像素验收，也不代替 API21 路径实测。

T13 API35 runtime1 原始日志记录 7,117 条 W/E/F；首次全新系统启动时，LMK 回收一次系统小区广播进程，时间早于 Trime 安装。未将其计为 Trime 崩溃，也未删除该记录。该轮 Setup 窗口未复现旧 `PhoneWindow` 焦点警告，但重建后的试打焦点检查确实失败；异步启用页面后的焦点恢复随后补充。该轮编辑器回归在前置标签断言处中断，未执行 WebView 清理，不能用来关闭旧 renderer 问题。完整范围和原件见 [T13 验证报告](VALIDATION-T13.md)。

新的隔离 userdata 也在安装 TalkBack／观察工具的阶段出现 3 次系统 `install_sessions.xml` rename 失败（runtime1 原日志 L38479、38502、38605，system_server PID610），因此不能把换干净磁盘当作修复。Android 15 的 [PackageInstallerService](https://github.com/aosp-mirror/platform_frameworks_base/blob/android-15.0.0_r1/services/core/java/com/android/server/pm/PackageInstallerService.java) 在锁内取得 session 列表后于锁外写文件；[AtomicFile](https://github.com/aosp-mirror/platform_frameworks_base/blob/android-15.0.0_r1/core/java/android/util/AtomicFile.java) 的该报错没有 errno。这只提供进一步核查并发写入和文件状态的方向，并不证明本轮唯一根因。所读上游源码原件及 SHA 保存在 `build/t13/api35-atomicfile-source1`；未改系统权限或掩盖错误。

Build25 的 Trime Manifest 已设置 `enableOnBackInvokedCallback=true`；“未启用／请修改 Manifest”的记录发自 DocumentsUI PID1992。当前 Trime PID 的 `sendCancelIfRunning(isInProgress=false)` 和单次 ImeBackDispatcher 注销缺 callback 应单独复核，不能宣称再加同一 Manifest 属性即可解决。

## runtime2 已复核范围

- **Setup／T10**：截至 22:57:41.743 的完整前缀中，Setup PID2808、T10 PID3223 均实际通过 14 项检查，宿主命令退出 0、原 instrumentation `passed=true` 且完成码为 `-1`；Setup 重建后的试打焦点恢复通过。两进程的 `PhoneWindow`、`RippleDrawable` W/E/F 均为 0，但 Setup 仍有 34 W／1 E，T10 有 11 W，不能称运行零警告。原件与逐组计数见 [Setup／T10 只读报告](../../build/t13/api35/runtime2-setup-t10-readonly-review1.json)。
- **IME 返回回调**：Setup 的唯一 E 在 22:55:52.247，callback ID `103925794`。此前同进程发生 Activity 重建、同 ID 的取消、关闭旧输入会话、`onFinishInputView` 和新 `onStartInput`。已读 [AOSP IME dispatcher](https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-15.0.0_r1/core/java/android/window/ImeOnBackInvokedDispatcher.java) 的对应分支：收到注销时列表内找不到该 ID 就记录此错误。项目未发现直接注册／注销误用；系统清理与异步注销交错是机制解释，尚无内部列表或调用栈证明唯一根因。实际镜像与所读 `android-15.0.0_r1` 并未建立精确二进制对应；不以提前隐藏键盘、跳过重建、反射或日志降级规避。见 [回调专项报告](../../build/t13/api35/runtime2-imeback-readonly-review1.json)。
- **安装会话写入**：截至 23:11:49.023 的完整前缀共保留 3 条 `install_sessions.xml` rename E：22:55:08.347 属于 Debug APK 安装；23:04:14.730、23:04:15.143 分别关联 Observer 与 EditorFixture 安装窗口，不能全部归给 TalkBack APK。system_server 均为 PID613；后两次记录线程分别为实际 `android.io`／`PackageInstalle`，相邻有另一线程的 package-session commit 日志。工具安装前后快照均实际返回 0，boot／system_server 身份一致，采样时 `/data` 可写、块与 inode 充足；22 字节 session 文件内容哈希相同但 inode 已替换，`.new`／`.bak` 均在采样时不存在。这支持既有并发写入调查方向；[RequestThrottle](https://android.googlesource.com/platform/frameworks/base/+/android-15.0.0_r1/services/core/java/com/android/server/pm/utils/RequestThrottle.java) 的调度与直接执行不提供写入互斥，但快照仍未取得失败瞬间 errno／FD，不能证明唯一原因或每次 session 持久化成功。首次 Debug 安装前采集失败原件也保留，未计为完成。
- **WebView 清理**：22:58:27.259–.262，Trime PID3340 的同 run／同 WebView identity 标记记录 remove／destroy 耗时约 3.26 ms；12 ms 后 Chromium 报 renderer3422 `crash detected (code -1)`，随后系统以 `isolated not needed` 清理该 renderer，Zygote 记录退出 0。夹具先移出 View 树再在主线程 destroy，符合 [WebView 公开契约](https://developer.android.com/reference/android/webkit/WebView#destroy())。实际报告版本 `124.0.6367.219` 的 [Chromium 对应源码](https://chromium.googlesource.com/chromium/src/+/124.0.6367.219/android_webview/browser/aw_browser_terminator.cc) 在 `normal_termination=false` 时才输出这条 E，因此不能因 Zygote 退出 0 就改称 Chromium 正常关闭。父 Trime 进程继续，真实 WebView 邮箱输入及完整编辑器回归、100 次显隐均通过；这仍不是 renderer 内部原因已闭合或整轮验收通过。

后两项的原始行、安装回执、快照哈希、版本限制及编辑器终态见 [安装／WebView 只读报告](../../build/t13/api35/runtime2-tool-install-warning-review1/report.json)。其日志前缀为 7,993,998 字节、55,881 完整行，仅覆盖采集时已有记录；后续 TalkBack 行为与运行事件不在该报告结论内。上述系统错误继续保留，没有“全部警告已修复”的结论。

## runtime2 关闭后的完整日志范围

完整原件共 60,433 行，保留 6,227 条 W、2,976 条 E、0 条 F（共 9,203 条）。既有模式匹配的 fatal／ANR、LMK、内核 critical 均为 0，不表示系统无错误。Trime 与 instrumentation 共用进程的 57 条 E 分为：51 条 FrameTracker 超时、4 条 EGL 配置失败、1 条 IME 回调未找到、1 条 WebView renderer crash；没有证据把任何一条解释为故意注入错误。测试通过也不能证明这些超时没有性能影响。详见[完整扫描](../../build/t13/api35/runtime2-independent-warning-scan1/report.json)和[进程错误分组](../../build/t13/api35/runtime2-trime-error-group-review1/report.json)；[FrameTracker／EGL 专项复核](../../build/t13/api35/runtime2-frametracker-egl-review1/report.json)已完成日志与官方机制对应：前者是平台 IME 动画结束后等待 SurfaceFlinger 回调超时，不能换算为键盘显示耗时或 P95。原 100 次压力路径确有 show 开始 7 ms 后请求 hide，保留该压力记录，不降低节奏来制造零错误；缺少回调的唯一原因仍未证实。四条 EGL 错误对应模拟器拒绝超出其能力的 ES 3.2／3.1 请求，真实 WebView 邮箱输入通过，但成功 GL context 与具体回退调用路径尚未捕获，不以关闭硬件加速或过滤日志规避。

## runtime4 关闭后的限定复核

冻结 check11 的完整保留日志共 4,485 W／2,098 E／0 F（6,583 条）；既有 fatal／ANR／LMK／内核 critical 模式未匹配。Setup PID2492 的 14 项检查、Editor PID3491 的 40 轮显示及 100 次显隐通过；Matrix PID3005 在首项透明宿主几何断言失败，原 native `false`／完成码 `0` 和外层退出 1 保留，ADB 命令返回 0 不代表验收通过。三进程未复现 PhoneWindow／RippleDrawable 软件绘制或 improper requestLayout W/E/F；系统其他进程的记录保留。

Setup 仍有 IME callback E1 与 FrameEvents E1；Editor 仍有 FrameTracker E54（3 show／51 hide）、EGL E4，以及实际 WebView remove／destroy 结束约 8 ms 后的 renderer E1。这些记录不构成新增、确定的 Trime 生产 API 误用证据，也不说明无性能或渲染风险。本轮两次 EGL 请求原文均为 ES3.2；不能套用上一轮一次3.2／一次3.1的具体结论。全日志未出现 AtomicFile／install_sessions，但工具安装范围不同且发生并非确定，旧 rename 失败原因仍未闭合。完整计数、原始行与范围见[本轮警告复核](../../build/t13/api35/runtime4-warning-review1/interpretation.json)。VM／控制器均正常退出 0，最低可用内存约 1.70 GiB；原警告未过滤，矩阵失败未改判。

## 焦点修复的语义边界

原 `trial_input` 已设置 `saveEnabled=false`，旧设备 probe 明确要求 **完成后的试打内容在重建时清空**，真实 Han `commitText` 才能完成试打。这些语义保持：不把程序性恢复当真实提交，不扩大文本持久化，不覆盖用户布局／简繁草稿或 checkpoint。Advanced 返回的同一实例文本、selection 和未提交输入仍须保留。

AOSP 的 [PhoneWindow 恢复链](https://android.googlesource.com/platform/frameworks/base/+/0daf210/core/java/com/android/internal/policy/PhoneWindow.java) 在层级恢复后立即查找先前聚焦 ID；[Activity 恢复链](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android13-release/core/java/android/app/Activity.java) 调用 Window 恢复。这支持上述时序解释；具体本轮修复效果仍以新设备日志和 probe 为准。

验收应分别报告“当前 Trime 警告是否复现”“隔离验收环境问题是否修复”“仍存在的系统警告”。新日志不得覆盖旧轮；当前阶段没有“全部警告消除”或新稳定版验收结论。


## API35 runtime5：check12 有限路径复核

原连续日志保留 4,093 条 W、2,351 条 E、0 条 F，共 6,444 条；既有致命／ANR／LMK／内核 critical 搜索均为 0，不代表所有系统错误消除。实际运行了 setup4 与两组竖屏矩阵，PID 分别为 2544／3057／3524；三次 ADB transport 0 与矩阵外层退出 1 分开记录。字体 2 原图的符号文字越界和旧页截图使视觉验收仍失败。

项目三个 PID 仍有一次 `ImeBackDispatcher` 缺失回调、两次 `FrameTracker` SHOW 动画监控超时。另保留一次 ART 对 Iconics 图标类的 107.200 ms 校验警告，这是 Debug 类加载记录，不能等同候选 P95。未复现项目 PhoneWindow／Ripple 软件 Canvas 错误；本轮未跑 editors／WebView／100 次显隐，所以不能关闭前轮 EGL／renderer 和连续显隐问题。没有新的确定应用代码误用证据，不以删除日志、关闭硬件加速或权限扩张来消除警告文字。

VM／控制器正常退出 0，最低可用内存 2,193,932 KiB。详见 [本轮逐项复核](../../build/t13/api35/runtime5-warning-review1/interpretation.json)；完整原件及失败保留。


## API35 runtime6：当前普通输入与独立诊断范围

完整原日志保留 4,322 W／2,556 E／0 F，共 6,878 条。既有 fatal／ANR／LMK／内核 critical／内核 WARNING 模式匹配为 0，仅对应已保留日志。Trime 相关 PID 合计 155 W／4 E：首次设置 1 条 IME 返回回调错误，前两组普通竖屏及第一次独立横屏诊断各 1 条 FrameTracker SHOW 超时；第二次独立横屏诊断还有 6 条 SurfaceFlinger missed-frame 警告。本轮没有执行 100 次显隐，不能借旧轮压力测试解释这些普通输入超时，也没有 P95 或唯一图形原因结论。

Settings 旋转锚点触发的 ResourcesCompat 警告来自 Settings PID1175；StrictMode 错误来自 DocumentsUI，实际 improper requestLayout 来自 SystemUI 时钟／移动网络图标。当前未发现 Trime 的同类记录，也未复现 AtomicFile，但这不证明旧安装持久化错误已消失。配置导入本轮未执行，不能据此宣称导入排队警告已修复。159 条项目记录及全部系统原件、PID／时间归属保留在[本轮限定解释](../../build/t13/api35/runtime6-warning-review1/interpretation.json)。扫描实际退出 0、内存上限 128 MiB、峰值约 22.5 MiB；不授予运行零警告或完整验收通过结论。


## API35 runtime7：横屏节点诊断轮

原日志保留 3,487 W／1,942 E／0 F，共 5,429 条；Trime 相关 PID 为 58 W／3 E（IME 返回回调 1、FrameTracker 2）。本轮仅 setup6 14 项通过及失败的 landscape-diagnostic3，未跑完整矩阵或 100 次显隐；fatal／ANR／LMK／内核 critical 的既有模式未匹配，不能扩大为风险消除。

本轮真实复现 1 条 `system_server` PID607 写 `install_sessions.xml.new` 时的 AtomicFile rename 错误（连续日志第 30,582 行），因此旧安装会话持久化问题仍未闭合，不能归为 Trime 已知调用错误或宣称解决。具体原文、PID 启动记录和有界扫描回执见[本轮限定复核](../../build/t13/api35/runtime7-warning-review1/interpretation.json)。扫描实际退出 0，128 MiB 上限，峰值约 22.6 MiB；原件保留。

API35 runtime8 原日志保留 3,501 W／1,950 E／0 F；Trime 相关 PID 为 58 W／1 E，仍有 IME 返回回调错误。本轮系统 LMK **1 次**：小区广播进程 PID1408／UID10140 因低水位与 thrashing 被回收（原日志第 20,267 行），早于 Trime 启动，不能记为 Trime 被杀，也不能因宿主可用内存未触发保护而写成零 LMK。setup7 的 14 项检查通过，landscape-diagnostic4 在同一 holder 回收夹具断言失败，尚未实际执行拼音动作；FrameTracker 0、AtomicFile 0 只对应这次有限范围，未关闭旧警告或证明修复。既有 fatal／ANR／内核 critical 模式未匹配；VM／控制器退出 0，最低宿主可用内存 2,161,444 KiB。扫描实际退出 0、128 MiB 上限，原件及差异见[本轮限定复核](../../build/t13/api35/runtime8-warning-review1/interpretation.json)；没有完整矩阵通过或警告批准结论。

API35 runtime9 保留 3,912 W／1,969 E／0 F，Trime 相关 PID 为 60 W／3 E：IME 返回回调 1 条、FrameTracker SHOW 超时 2 条；系统 PID605 的 AtomicFile 安装会话 rename 错误再次出现 1 条。LMK 0 仅对应本轮保留日志，不撤销 runtime8 系统 LMK 1 的记录，也不关闭这些系统警告。setup8 14 项通过，横屏诊断已完成拼音锁定，但符号页 Return 完整可见等待超时，截图 0、完整矩阵未跑。扫描实际退出 0、128 MiB 上限；原件、计数及失败见[本轮限定复核](../../build/t13/api35/runtime9-warning-review1/interpretation.json)，没有警告批准结论。

runtime9 两条 FrameTracker E 均有实际 CUJ 80 begin／end 请求，分别在结束请求后约 10.3／10.7 秒出现。官方机制是在动画结束后重试获取 SurfaceFlinger 数据，再以 10 秒 deadline 强制收尾；这是统计回调等待，**不是输入显示耗时或候选 P95**。同轮冷 Debug 首次绘制的 HWUI 754／1,682 ms，以及后者跳过 77 帧，作为独立卡帧证据保留，未据此证明回调缺失的原因。缺少 surface 身份、结束帧与回调栈，尚不能唯一归为项目调用错误，也不判系统告警良性。原始行号与官方源码 SHA 见[有限上下文复核](../../build/t13/api35/runtime9-frametracker-context-review1/report.json)；先完成修复与矩阵，Release 冷启动必要路径随后实测。


## API35 runtime14：完整日志与矩阵失败范围

原日志保留 **7,433 W／4,399 E／0 F**，其中 Trime 相关进程为 **281 W／8 E**。8 条 E 包括 7 次 FrameTracker IME_SHOW 动画统计收尾等待超时，以及 setup13 PID2513 的 1 次 ImeBackDispatcher 回调未找到；前者不能换算成键盘显示耗时或候选 P95，后者仍无唯一归因。项目另有 20 条 FrameTracker 丢帧／预测错误 W。当前未匹配项目 PhoneWindow、RippleDrawable 或 FrameEvents W/E，但不代表这些路径和应用风险已全部消除。

系统 AtomicFile 再次出现 **1** 条真实 rename E：原日志第 30,651 行、设备时间 03:17:36.359、system_server PID611／TID690，关联测试 APK 安装窗口及相邻 package-session 提交。没有失败瞬间 errno 或并发调用轨迹，根因仍未确定；安装成功不能代替持久化健康检查。全部外部警告保留。既有 fatal／ANR、LMK、内核 critical 模式均未匹配，仅对应本轮留存日志范围。

setup13 的 14 项检查及独立大字体横屏诊断通过；matrix9 前七配置通过，第八配置在省略文本夹具断言失败，原 native `false`／完成码 `0`、Probe 退出 1 与整轮失败保留。配置恢复完成、恢复错误为空；VM／控制器实际退出 0，376 个内存样本最低 1,312,952 KiB。扫描实际退出 0，以 128 MiB 上限、nice 10 运行，峰值约 23 MiB。逐进程归属、原始行及限制见[扫描报告](../../build/t13/api35/runtime14-warning-review1/report.json)和[限定解释](../../build/t13/api35/runtime14-warning-review1/interpretation.json)；没有运行零警告或完整验收通过结论。


## API35 runtime15／16：配置通过与内存中止的范围

runtime15 的 check23 APK 完成 setup14 的 14 项检查和 matrix10 八项配置，各配置 31 项检查、两主题共六张原图。系统夜间模式仍使用默认浅色主题配色，不能据此宣称深色像素验收。公开选择 Steam 后的深色补测在安装身份检查阶段被宿主内存保护中断，未开始任何原生配置检查。原日志为 **6,836 W／3,828 E**，项目进程 **255 W／9 E**；项目 E 为八条 FrameTracker SHOW 统计收尾超时及一条 ImeBackDispatcher 回调未找到，没有新的唯一应用代码归因。AtomicFile、本轮 fatal／ANR／LMK／内核 critical 模式均为 0，仅限留存日志，不撤销旧错误。VM 正常退出 0、控制器退出 1，最低可用内存 1,022,648 KiB。详见[本轮限定复核](../../build/t13/api35/runtime15-warning-review1/interpretation.json)。

runtime16 已安装并读回核对 check24 的主应用与测试 APK，但 setup15 只有启动和 IME 选择状态，没有 14 项检查完成结果。宿主可用内存最低 **1,001,532 KiB**，保护终止了测试 ADB，随后正常关闭模拟器；控制器退出 1、VM 退出 0、清理错误为空、没有残留。原日志为 **2,920 W／1,694 E**，项目进程 **19 W／1 E**。唯一项目 E 是 PID2484 的 Binder 事务 `115399/-22`（原日志第 31,746 行）；相邻内核记录明确目标节点不存在，但未提供可确定对端身份或调用栈。该事件晚于测试 ADB 中止、早于正常 emulator kill，不能仅凭时序将其认定为 Trime 崩溃，也不能宣称它必然由清理造成。既有 fatal／ANR／LMK／内核 critical 模式均为 0，项目 FrameTracker、ImeBackDispatcher 与系统 AtomicFile 本轮未复现；这次短轮不足以关闭旧问题。详见[本轮限定复核](../../build/t13/api35/runtime16-warning-review1/interpretation.json)。

两轮扫描均实际退出 0，以 128 MiB 上限运行，峰值约 23 MiB；原始日志未删除。宿主保护不是客体 LMK。check24 新增的 39 项触摸／无障碍交错回归仍待设备执行，不能用 check23 的矩阵结果替代。

## API21 runtime2／3：前置失败与真实产品崩溃

runtime2 保留 **1,530 W／563 E／0 F**，项目归属 W/E/F 为 0；setup1／2／3 均在 instrumentation 业务启动前失败，分别为旧 shell 无 `printf`、`id -u` 返回完整 UID 文本、SD 介质缺失导致实际目录只读。没有候选、无障碍或输入功能通过证据。系统 Resources、驱动／配置、字体及 Calendar StrictMode 原记录全部保留；fatal／ANR、LMK、内核 critical 的既有模式未匹配，不表示系统无错误。[扫描](../../build/t13/api21/runtime2-warning-review1/report.json)实际退出 0（b39778），128 MiB 上限；[限定解释](../../build/t13/api21/runtime2-warning-review1/interpretation.json)明确这些工具／介质前置失败不等于 Trime 业务失败。

runtime3 保留 **1,775 W／1,104 E／0 F**，项目进程 **16 W／59 E**。`FATAL EXCEPTION` 本次记录在 E 等级：原日志 **L7573–7601**、设备时间 `04:27:01.852`、setup4 PID **2766**，`IllegalStateException: Session setup-closed-loop-… is not established`，链为 `Key.checkKeyAction → KeyView.accessibilityAction → refreshAppearance → KeyboardWindow.onRimeOptionUpdated`；L7630 另有同 PID、同异常的 **am_crash 1**。它发生在宿主 instrumentation 完成之前，稍后 L7602 的 finished-instrumentation 主动停进程不能抹掉此前真实崩溃。setup4 的 14 项／true／−1／host0 原件不改，但**不得记为验收通过**。这是可修的 UI／session 生命周期访问缺陷；随后已完成缓存读取与 typed session 关闭处理的源码修复和独立审阅，但尚无新编译或设备复验结果。check27 的一小时内存等待已退出 124，未启动 Gradle，不能声称崩溃已消除。[原栈上下文](../../build/t13/api21/runtime3-warning-review1/setup4-crash-context-originals.json)与[限定解释](../../build/t13/api21/runtime3-warning-review1/interpretation.json)均保留。

项目其余记录包括 **18** 条 SQLite WAL recovered、**12** 条 eglCodecCommon 未知 GL 参数 E、4 条 OpenGLRenderer 配置回退 W、8 条 ART W、1 条 SpellChecker W，以及 matrix4 的 **3** 条 inactive InputConnection W；现有记录未提供这些类别的唯一调用归因，WAL 原文不等于词库损坏，GL／连接错误也未被证明无害或已修复。matrix4 的精确 scroll13 夹具失败与 setup4 fatal 分别记录，尚未执行 39 项候选回归。[扫描](../../build/t13/api21/runtime3-warning-review1/report.json)实际退出 0（fece53）、128 MiB 上限，原件未变；本轮 LMK／内核 critical 模式 0 仅限留存日志。Root 正常 STOP、控制器／VM 退出 0、无残留及最低可用内存 1,454,560 KiB，不替代无崩溃验收，也不构成警告批准。

## API21 GLES 能力限制：仅源码调查，未执行

runtime3 的 12 条 GL 参数错误涉及 `0x82DA = GL_MAX_VERTEX_ATTRIB_BINDINGS`（GLES3.1）、`0x8CDF = GL_MAX_COLOR_ATTACHMENTS`、`0x8824 = GL_MAX_DRAW_BUFFERS`（后两者为 GLES3.0），见 [Khronos 定义](https://raw.githubusercontent.com/KhronosGroup/OpenGL-Registry/main/api/GLES3/gl31.h)。官方提供 `GLESDynamicVersion = off` 兼容设置；[emulator 源码](https://android.googlesource.com/platform/external/qemu/+/430b0d497ee9495d6c20e99b4d729c636d6df86c/android/android-emu/android/help.c)也确认 `-feature` 可用负号禁用、逗号列举，因此仅隔离诊断可考虑将原参数组合为 `-feature -Vulkan,-GLESDynamicVersion`，保留 swangle、硬件 Canvas 路径及全部功能断言。[官方发布说明](https://developer.android.com/studio/releases/emulator)

本机 SDK 36.5.11 随附的 `emulator/lib/advancedFeatures.ini` 已写 off，但 runtime3 原 `emulator.log` L39／L56 仍记录 ES3.1／`qemu.opengles.version=196609`，不能将随附默认值视为本轮实际生效值，覆盖来源尚未核定。限制至 GLES≤2 会减少 GLES3 覆盖，只能作诊断对照，主验收保留原配置；当前未启动模拟器、未改 SDK／镜像／冻结工具，未验证限制效果，也不声称 12 条错误已消除或系统问题已闭合。


## API21 runtime4：旧fatal未匹配，设备检查仍失败

[扫描](../../build/t13/api21/runtime4-warning-review1/report.json)实际子进程wait0（3a6baa），nice10／128MiB、峰值22,252 KiB，原输入前后绑定一致；原日志保留**1656W／745E／0F**，项目**8W／14E**。项目E为SQLite WAL恢复8条、旧guest GL参数6条，W为OpenGL配置回退2、ART3、SpellChecker1、inactive InputConnection2；不能因数量下降便批准或宣称修复。全PID fatal／am_crash／am_anr／旧Session异常及真实LMK kill／内核critical模式均未匹配，只是本轮有限未复现；lowmemorykiller L40／41是能力／支持诊断，另有ActivityManager连接通知，三条均非真实kill，缺少内存压力支持的E仍保留。[有限解释与原文](../../build/t13/api21/runtime4-warning-review1/interpretation.json)和[实际执行回执](../../build/t13/api21/runtime4-warning-review1-execution1/execution-receipt.json)均已保存。

check28已通过812项新JVM（含7项typed关闭回归），但setup5只在原14项与新增缓存前5项后，由测试捕获detach KeyView无障碍节点mAttachInfo NPE，native false／完成码0、宿主1；这不是未捕获进程fatal，也不是通过。matrix5键盘21／原scroll13及4阶段通过后，候选next-page-is-labelled-button断言失败、0图／宿主1，设置恢复true且错误为空。VM／控制器正常退出0、最低可用内存3,332,876 KiB，不改变两项失败；后续缓存边界／候选39／完整矩阵尚未完成，旧runtime3 fatal和系统警告不关闭。check29源码修正中、未构建，未改原日志或降低警告门槛。


## API21 runtime5：新ThemeUtils夹具错误与非内存中断

[扫描](../../build/t13/api21/runtime5-warning-review1/report.json)实际子进程wait0（3a265e），128MiB／nice10、VmHWM22,404 KiB，原件绑定前后一致；保留**1791W／897E／0F**、项目**15W／34E**。新增ThemeUtils **12E**来自两个矩阵PID3302／3852：BoundActionTextView、QueuedTouchTextView和verifyRebindEvents匿名夹具各4条，原文要求Theme.AppCompat。Root已在3个夹具构造点补有效主题ContextThemeWrapper，改善类型名／注释；已随check30编译，尚未设备复验，不记错误已消失。其余项目E为SQLite code283 WAL恢复10条和旧guest三GL enum共12条，inactive InputConnection共5W，均不以改日志或缩小覆盖消除。完整原文／行号见[有限解释](../../build/t13/api21/runtime5-warning-review1/interpretation.json) SHA `d868692d0681769262cb379d3b627d061652043e35612f99099a0fb1a5f101e4`。

本轮fatal／am_crash／am_anr／旧Session异常、真实LMK kill及内核critical模式均0；lowmemorykiller的两条能力诊断及一条连接消息不是kill，仍保留其系统限制。runtime5无STOP，控制器因signal15退出1、VM／logcat0、清理空、最低3,408,596 KiB，非1GiB阈值停止，信号来源未知。setup6原14＋缓存7项有真实输出但可选第8项未覆盖；matrix6第一组通过、第二组请求字号2实际1而拒收，编辑器无native终态／报告pull、不计100次显隐，整轮不通过。check30编译通过（812项JVM沿用check29、无本轮Lint／Release）及字体helper宿主javac／D8零警告均不能替代这些设备范围，旧runtime3 fatal和全部警告原件不改。


## API21 runtime6：ThemeUtils限定未复现，原系统诊断仍保留

[扫描](../../build/t13/api21/runtime6-warning-review1/report.json)实际e0c8eb／子进程wait0，128MiB／nice10、VmHWM22,964 KiB，原输入绑定前后相同。完整保留**2177W／1320E／0F**、项目**149W／92E**；本轮无ThemeUtils tag，四组矩阵确实执行了check30的主题包装夹具，限定验证runtime5的12条同类E未复现，不扩大为所有上下文已修。全PID旧Session异常、fatal／am_crash／am_anr、真实LMK kill和内核critical模式0；三条lowmemorykiller能力／连接消息非kill，系统限制仍留存。

剩余项目消息包括feedback PID6943的AudioTrack **105W**（`AUDIO_OUTPUT_FLAG_FAST denied by client`）、eglCodecCommon **60E**（三旧enum各8次共24，另editors／WebView PID6338的`0x85B5`共36）、SQLite code283 WAL恢复 **32E**、inactive InputConnection **6W**，以及Chromium locale／代理／index、已detach AwContents、权限／方向和ART等原文。测试通过不证明驱动无害、音频低延迟或数据库完整性，也不以扩大权限／关闭硬件Canvas／过滤日志规避。[有限解释](../../build/t13/api21/runtime6-warning-review1/interpretation.json) SHA `10576036bcd8dc5b64fc2fddb5f163613da6f6252430506af390555196329894`及[实际扫描回执](../../build/t13/api21/runtime6-warning-review1-execution1/execution-receipt.json)保存全部绑定；解释writer首轮把4条I级WebView标记误计入W/E的自有断言失败也透明保留，原scanner实际0／原件不变。

setup7、四组矩阵、完整editors及五项串行回归共11个probe阶段各有native true／−1和宿主0；Root正常STOP后controller／VM0、cleanup空、最低3,253,812 KiB。不作为最终T13／Release或warning批准，不撤销runtime3真实fatal与runtime5未知signal15。当前10处配色和9.16版本身份是未构建的本地代码候选，尚未推送或发布；后续必须按最终APK重新绑定两平台和日志范围。
