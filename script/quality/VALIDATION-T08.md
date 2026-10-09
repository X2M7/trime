# T08 候选延迟与内存预算验证

状态（2026-10-09）：实现与本地验证已完成，最终 Debug/Release 构建、回归和在线 Lint 均通过；真机正式构建性能验收未完成。没有真机 P95/PSS、准确率对照或首次部署结果，不能宣称达到预算。源码和报告同步范围为 X2M7/trime 的 develop；未创建新标签或 Release，稳定版 Latest 保持 v3.3.13-t9.10。

## 实现范围

- 请求关联使用编辑器 token、单调快照 generation 和可选 request ID。即使日常关闭性能日志，候选行仍保留原快照及全局索引；选词、忘词、翻页及展开分页在 Rime 单线程中核对归属，拒绝旧结果。编辑器提交/按键消息保持原有串行队列和有背压消息总线，没有输入节流或丢弃。
- 延续原 RimeResponse 的一次 JNI 聚合快照，原 JNI 构造签名保持一致；既有 T9 聚合查询没有改为逐候选查询。展开候选单次最多读取 48 项，`PagingConfig.maxSize=144`（分页保留目标），并修复已知总数时最后一项候选无法继续加载的问题。这是分页缓存目标，不是整个候选 View 的内存上限。
- 默认关闭 TrimePerf 日志；通过 `-Ptrime.performanceTrace=true` 可在真实 release/R8 构建中启用。记录反馈调用完成、IME 提交、进入 Rime 队列、开始执行、聚合计算结束、候选子树 draw 和编辑器成功提交的时间。记录只有数值，不包含输入、按键、候选文字或编辑器应用名；隐私模式不采样。
- 主 P95 使用带 KeyView 反馈关联的请求提交到非空候选 draw。空列表绘制为 candidate_clear，按键/控制请求的候选显示延迟及结算数量分别统计；队列、计算和编辑器提交延迟仍为总体汇总，直接/native 提交未拆成独立分布。直接提交不作为候选显示样本。draw 回调代表应用绘制提交，不证明像素已经呈现；反馈到显示还包含按住按键的时间。
- 诊断请求缓存有界为 256 项，淘汰不改变输入队列，也不把缺失测量伪装成取消。补充修复了已淘汰的直接提交可能先记录成功、再误记取消的边界；淘汰后不推断取消，真实失败仍照实记录。旧显示快照、编辑器切换、成功直接提交、普通取消及实际失败分别结算。解析器要求同编辑器的最新已完成 generation，commit 仍允许按序回写原快照。
- KeyboardWindow 只增加已缓存布局和按键数量统计，没有改变布局缓存策略或新增资源。当前 InputFeedbackManager.init() 只做初始化标记；TTS 在允许朗读时创建，SoundPool 在启用自定义音效且音频路径列表非空时创建，因此本次不重复重写延迟初始化。

## 本地验证

最终源码提交为 `3499398a`。当前最终检查日志位于 `build/t08/local-validation-5/`；Debug 阶段实际 BUILD SUCCESSFUL，11 分 4 秒，112 tasks（106 executed / 6 up-to-date），598 项应用 JVM 测试全部通过（59 suites，failures/errors/skipped 均为 0），9 项 build-logic 测试也在新工具链下重新执行通过；Spotless 与在线 Debug Lint XML 0 issue，两种 ABI 的 Debug APK 及测试 APK 编译、打包通过。测试 APK 尚未运行于设备。新增 5 项纯 JVM 回归实际注册 257/300 个诊断请求，验证淘汰、迟到成功提交、真实失败、正常取消及编辑器归属。

Release/R8 与在线 Release Lint 实际 BUILD SUCCESSFUL，8 分 16 秒，63 tasks（52 executed / 11 up-to-date）；Release Lint XML 同样 0 issue。`local-validation-1` 保留初次类型推断/协程导入的编译失败，`local-validation-2` 保留 5 项 Lint 失败，`local-validation-3` 保留 Release 原生编译后因离线缓存缺少 Splitties release AAR 而失败的记录，`local-validation-4` 保留 Release assemble/R8 通过但在线 Lint 因 6 项版本通知失败的记录及完整报告；均不计最终全部通过。依赖补齐后保留原版本的 Android 运行库约束；没有改成离线来消除在线提示。KTX 写法已修正；性能日志只在一个数值输出入口精确豁免 LogNotTimber 风格规则，因为现有 Timber 树会改写固定标签，不能替代 TrimePerf 协议。

第 5 轮采用 Gradle 9.8.1、Kotlin 2.4.21、Spotless 8.10.4。Gradle 9.7.1 落在官方三项高评级漏洞的影响范围，已升级至公开修复版；官方发行 ZIP 的 SHA256 经独立流式验证并固定于 wrapper，同时更新 wrapper JAR/脚本。Paging 3.5.2 的官方 runtime AAR 要求 minSdk 23，3.3.6 为 21，因此保持严格固定 3.3.6，仅更新已审查坐标/版本对的精确通知规则；其他坐标或建议版本变化仍会报错。没有整体禁用版本检查。依据见 [API 21 约束](API21.md)、[Gradle 官方安全公告](https://docs.gradle.org/9.8.1/release-notes.html)。

Gradle 升级后存在 Configuration.setVisible 弃用提示。Debug 日志和独立堆栈审查定位到 AGP 9.4.1（52 次）与 KSP 2.3.11（3 次），Release 也记录 55 次同类提示，wrapper/格式阶段记录 1 次。仓库构建脚本未调用这个 API。它是上游插件在未来 Gradle 11 的兼容性维护事项，不是本轮已消除的警告；原始堆栈完整保留，不关闭提示，也不回退到受影响的 Gradle。

两阶段构建期间 525 个选定源码/构建输入的 SHA256 前后相同，清单 SHA256 为 `94eea51c6b7d48a3c7742bcb05b448c5bf221ac7800f7064c0825fb53dff561c`。构建参数、21 个干净子模块的提交、JUnit/Lint XML、Debug APK、Release APK 及核验记录保存在 `build/t08/local-validation-5/`。代码身份为 `3499398ac122ef2ad4c07de388314e3a0d388302`，后续文档提交不改变这些编译输入。

两个本地测量 APK 为真正的非 debuggable Release/R8 构建，`PERFORMANCE_TRACING=true`、minSdk 21，ABI 与 ELF machine 检查通过。R8 输出保留了所需事件字符串，但这是静态检查，不能证明设备已经执行埋点。它们使用现有 Android debug/test 证书签名供隔离测量，不是新稳定版；versionName 仍为 3.3.13-t9.10，构建标识为 `v3.3.13-t9.10-4-g3499398a`。

| 本地测量 APK ABI | 字节数 | SHA256 |
| --- | ---: | --- |
| arm64-v8a | 11785118 | `e0f8938813aaf7cd0a8d5e2a8a3631a42a80339445eca4d2945781a58fee9d85` |
| x86_64 | 11862939 | `6f9a2050e74699cab2f98722c91c02267654d5b087a7d46bc1d04bd51c747b71` |

冻结文件位于 `build/t08/local-validation-5/release-test-signed/`，签名验证成功。每个 APK 仍有 2 项 META-INF/services 签名警告，涉及 AndroidDispatcherFactory 与 AndroidExceptionPreHandler；原文保存在相应 `*-signature.txt`。没有删除服务入口或宣称全部签名风险已消除。冻结脚本首次因 aapt2 使用 minSdkVersion 字段而停止的本地诊断也保留，修正解析后重新核验通过，没有因此修改 APK 源码。

Python 性能工具 31 项测试已实际通过，包括旧 generation/旧编辑器、跨请求显示新鲜度、消息乱序、候选快照显示关联、直接提交、任务失败不能被合法绘制掩盖、丢失测量不能自动结算、3000 次快速空刷新不能满足候选预算、PSS 与 RSS 区分、未知 graphics、驻留增长、采样配置一致性及准确率回归。它们使用合成数据，没有运行真实 PagingSource/View，也不是设备性能结果。

新增 Android 调用兼容 minSdk 21：elapsedRealtimeNanos 为 API 17，isLaidOut 为 API 19。关闭追踪时跳过请求分配、时间采样、日志、绘制子节点遍历与缓存计数；候选关联元数据和少量调用封装仍有成本，不能声称零开销。

editor_commit 记录 commitText / finishComposingText 的成功返回。未处理的空格、回车通过 sendKeyEvent / performEditorAction 转发时，不计为这类提交，当前没有编辑器动作的独立结束时间；不能声称覆盖全部编辑器响应耗时。

本轮限制 Gradle JVM 为 Xmx1536m/Metaspace768m、单 worker、Kotlin in-process，CMake 编译池和链接池深度分别为 1，允许一个编译和一个链接同时执行；两种 ABI 的构建串行。AGP 直接调用 Ninja，限制通过现有 CMake job pools 生效，不能仅据环境变量声称所有 Ninja 任务全局串行。没有启动模拟器。多轮构建的 633 个主机样本中可用内存最低 5751.5 MiB，Java 进程 RSS 合计最高 3175.9 MiB；这些是采样值，不是瞬时完整构建峰值或 Android PSS。构建和签名完成后已核对没有 Java/模拟器进程残留，内存采样器已停止。

## 尚未完成的验收

本轮最后 ADB 检查无连接设备。连接同一台真机后，使用正式非 debuggable 构建与相同工具链、埋点、方案/词库起点、反馈设置、输入语料和主题切换协议，交替执行基线与候选。日常 Debug 构建和过去 T07 的模拟器验收不计入 T08 真机性能结果。

- 非空候选显示至少 1000 个样本，初始目标 P95 ≤ 100 ms；若基线明确超过 100 ms，可先达到至少 30% 的改善，并保留最终目标。
- 同 PID/启动周期连续至少五轮，分别记录 Java、native、graphics 与总 PSS；比较每轮最后三个静置样本的中位数。暂定驻留增长预算为 max(4 MiB, 起始驻留的 5%)，不是通用峰值内存上限。设备不提供 graphics 记账时记为未知，不能算通过。
- 核对固定语料的完整提交、候选准确率/排名、退格、候选选择、编辑器和主题切换；首次部署用独立起止及可输入时间记录，单独报告。

当前 Rime.startup 默认 fullCheck=false，调用链把实际参数传给 native start_maintenance。未找到 BuildConfig.DEBUG 自动强制 true；历史 fullCheck=true 的维护全量检查不能代表正式版普通启动，也不等于每次弹键盘全量重编译。

采集器只读、限制日志与分析事件数量，核对安装 APK 散列并拒绝已知模拟器和 debuggable 包；这些检查仍需人工确认实物及源码/签名来源。操作步骤与边界见 [T08 测量说明](t08/README.md)。

参考：[Android 时钟](https://developer.android.com/reference/android/os/SystemClock#elapsedRealtimeNanos())、[dumpsys meminfo](https://developer.android.com/tools/dumpsys#meminfo)、[View 绘制回调](https://developer.android.com/reference/android/view/View#dispatchDraw(android.graphics.Canvas))。
