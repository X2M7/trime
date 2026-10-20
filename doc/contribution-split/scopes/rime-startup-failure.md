# 下一组范围审阅：Rime 单线程队列与失败处理

状态：仅源码范围设计，未实现、编译、测试或推送。本文件仅为 fork 拆分索引的范围设计，不属于任何源码分支或上游补丁。

审阅基准：`f5008260dc1e71b05e8e5508c6cfbf8ebf2507a0`。参考实现：`df12d0f4f62d7952a181e0ff7cf3cfd3e0077b7c`。下列行号是基准或参考提交中的行号，不是未来补丁行号。

## 建议第一项用户问题与边界

建议题目：`fix: complete waiting Rime calls when engine startup fails`。

触发是资产准备/原生启动抛出可恢复异常：原 dispatcher 没有 finally，可能跳过 nativeFinalize、保留 running 状态并使内部 scope 失败；lifecycle 留在 STARTING，`runOnReady` 等待 READY 没有失败出口。用户可能看到输入法一直未就绪，后续调用也不能明确结束。

第一 PR 应让一次启动失败形成完整终态：保留错误原因、结束已排队的协程等待、终止 ready 等待、完成 worker 清理，后续现有显式启动路径能创建新周期。正常运行仍使用现有单线程 executor；正常停止时关掉新任务准入、处理已经准入的队列项，再完成 native 清理。

不能只复制最终 `RimeDispatcher.kt` 然后宣称应用已经恢复：若 lifecycle 未接 FAILED，`whenReady` 仍会悬挂；若拒绝任务改用 IO fallback 但 `Rime.withRimeContext` 不采用线程守卫，NonCancellable 路径可能在错误线程进入已停止的 JNI。

本项不承诺 UI 降级面板、自动重试、晚到存储恢复、编辑器隔离或整条输入链无丢失。它也不能保证处理 JVM 致命错误、进程崩溃或 JNI 的 native crash。

`git grep` 核对基准 main/test 中的 `RimeLifecycle.State` 使用者，只有 Rime、RimeLifecycle 和 RimeDaemon；新增 FAILED 不要求顺带复制最终应用 UI。第一项生产代码实际集中于四个现有文件。

## 必须一起交付的最小闭环

| 文件/锚点 | 需要的 hunk | 不要搬入的部分 |
| --- | --- | --- |
| `RimeDispatcher.kt` 基准 start/stop/dispatch；参考 start 约 113–172 | 同一 submission lock 下原子准入/停机；start 返回时已建立本轮 stop completion；每轮独立 wakeup；try/finally 清理；失败后拒绝等待项；failure callback；停止/重启屏障；runConfined 的实际 worker 线程守卫 | 改线程数、输入节流、InputRequest/InputLatencyLog、新通用队列框架 |
| `RimeLifecycle.kt` 基准 emitEvent/whenAtState；参考 fail/FAILED/whenAtState | FAILED + cause；fail 时取消本周期子任务；whenReady/whenAtState 有失败出口；允许后续显式开始时清理前一失败原因；失败与正常 shutdown 的竞态规则 | editor token、候选身份、隐私状态、schema/词库功能 |
| `Rime.kt` 基准 dispatcher controller 62–75、withRimeContext 91–93 | nativeFailure 的最小实现：记录/发布 lifecycle 失败、撤销本实例 handler；withRimeContext 仅调用 dispatcher.runConfined | 最终 withRimeContext 的 InputRequest、activeRequest、延迟打点；clearTransientEditorState 中 T9/候选/隐私内容 |
| `Rime.kt` 基准 startRime 280–299、startup 444 左右、finalize 460 左右 | 原生初始化所有权/幂等清理；清理不能在 DataManager.sync 抛错且 JNI 尚未初始化时盲调 finalize；startup/normal-finalize 识别 FAILED，并等待本轮清理屏障；完成后按实际状态转移/撤销 handler | 最终 startRime 的缓存回退、预测/置顶、词库恢复、配置导入、fullCheck 参数重设计；不复制 beginShutdown/finishShutdown API 及使用者整块 |
| `RimeDaemon.kt` 基准 createSession 86–97、destroySession 99–108、restartRime 141–160、stopRime 162–164 | 仅为既有同步生命周期入口补 FAILED 分支；失败后不能永久跳过新启动，也不能从 onDestroy 把恢复性错误直接抛成应用崩溃；通知等待在失败时也应结束 | 参考实现 491 行增长里的 lifecycleChanges worker、30 次存储轮询、manual retry、Direct Boot process exit、ConcurrentHashMap/Session owner/cached reads、日志采集变更 |
| `Rime.kt` 基准 showAsciiSwitchTips 428–432 | 检查/约束延迟 tip 的原生读取：原代码在 Default lifecycleScope 延时后直接 getRimeContext。失败/停止时取消旧 tip，并让该原生读走现有 confined 入口 | 不搬参考实现依赖 editorToken/privacy 的整套 tip 改造，不顺带增加延迟统计 |

这是设计清单，不是可以机械套用的补丁：FAILED 与 shutdown/retry 时序需在最小代码上重建和测试。可以继续保留现有 `startup()` / `finalize()` 的公开签名，不需要引入全部异步 daemon 架构。

`RimeUnavailableException` 是一个带原始 cause 的取消异常，可沿用参考实现的最小定义；不要将一般业务异常都转换为该取消异常。普通候选/配置参数校验失败并不自动意味着 engine 已不可用。

## 精确剥离规则

- `RimeApi.kt` 保持原接口，本项无需添加 T9、预测、配置导入、词库维护、editor begin/finish 或候选快照 API。
- `RimeSession.kt` / `daemon/Extensions.kt` 不随第一项复制。参考版本的 typed `RimeSessionClosedException`、每客户端 scope、same-name session identity 与 launchOnReady 转取消，解决的是已关闭 owner 的排队行为，应另提。
- `RimeDaemon` 的异步 worker 是后续“创建/销毁 session 不在主线程等待引擎停止”项。原 synchronous lock/stop 可能仍有 UI 等待；第一项不得声称已经消除这种等待或 ANR。
- `System.loadLibrary` 从 companion init 移到 worker、`RimeSchema.empty()`、主题异步准备、降级键盘、晚到 storage predicate/重试组成另一闭环。保持原 eager load 时，本项捕获不到 Rime 类初始化阶段的链接错误，不能宣称覆盖全部 JNI 加载故障。
- 基准 `deploy()` / `updateConfig()` 内的 exit/start 失败不一定冒出 dispatcher 的 Runnable.run：协程会把异常交给它自己的调用者。若第一项只覆盖 nativeStartup/nativeFinalize，不能声称部署重启失败也已恢复；后续提取 `restartNative` 的显式 dispatcher.fail 包装时，只替换原有两个 restart 调用点，不搬导入事务。
- `RimeMaintenanceMutex` 及导入/词库事务不属于队列准入修复。后续异步 daemon 若要与部署互斥，才单独审阅这一排他关系。
- `RimeMessageBus` 是消息交付主题，不是 dispatcher 准入主题。基准 `MutableSharedFlow(extraBufferCapacity=15, DROP_OLDEST)` 仍可能丢输出。不能凭 worker 队列修复宣称提交、按键消息或候选 UI 端到端无丢失。
- 不引入输入事件节流、响应合并、EditorGeneration、CandidateSnapshot、PrivacyModeState、RuntimeOptionCache、T9/预测 native guard、主题重写或包/版本/发布配置。

## 不应夸大的顺序语义

FIFO 的可证明对象是实际准入的 Runnable/continuation；协程在挂起点可以交错，不能自动升级成“一个含多个 suspend 的事务整体原子”。正常 stop 的队列处理也不等同于每个已取消协程的业务体一定执行：lifecycle 的 ON_STOP 本来会取消其子任务。

失败后没有 Job 的 raw Runnable 无法用 coroutine cancellation 通知其调用者。参考 `WrappedRunnable.reject` 对它直接返回；拒绝 dispatch 的 IO fallback 又可能执行无 Job 的 block。因此对裸 dispatch 必须说明边界，应用原生调用统一通过 runConfined 守卫；不要把测试中的 raw Runnable 压力用例推广成任意调用者都获得失败结果。

worker 只应在准入/队列操作期间持 submission lock。发布 failure、等待订阅者或做慢清理时不能持有该锁，否则 UI 上的另一次 dispatch 可能互相等待。stop completion 要在 failure callback 真正结束之后完成，restart 不能抢在其前面。

`stop()` 从本 worker 自己调用会等待自己退出，存在自锁风险。现有 daemon 通常从外部调用；最小实现应明确禁止/拒绝这一调用方式并测到，或给出不自锁的行为，不能无说明地依赖调用者永不重入。

## 第一 PR 的聚焦测试设计

使用实际生产 Dispatcher/Lifecycle，fake controller 注入启动/清理行为，CountDownLatch/CompletableDeferred 控制时序；不加载 Rime JNI，不用长 sleep 或重写队列模型。所有 executor/scope 必须在 finally 关闭，测试超时只作失败保护。

1. **启动失败释放等待**：先阻塞启动，排入实际 coroutine 请求，再释放并抛出固定 IOException。验证业务体未执行、请求有界终止、原始 cause 保留，清理恰一次，随后显式新周期能成功。
2. **正常 FIFO 与停止屏障**：启动门闩后准入标号 1…8 的任务，关闭准入并释放启动。断言精确顺序 `[startup,1,…,8,finalize]`；不能仅用计数 8 代表顺序正确。重启后没有上轮任务。
3. **启动/stop 交错**：启动进入控制门闩，另一线程 stop，验证清理前 stop 不完成、释放后完成。若声称覆盖“worker 尚未取得锁”特定窗口，必须提供真正可控制的窗口，不能只靠调度快慢或测试名称。
4. **拒绝与取消**：停止前/后提交协程，等待实际 Job 结束而不是仅检查 dispatch 返回。取消的原有 cause 不应变成成功或无关异常。
5. **NonCancellable 回退安全**：从外部 executor 在 stopped dispatcher 调用真实 runConfined；即使外层 NonCancellable，也不得执行模拟 native body 或在 IO 上运行它。
6. **启动/清理双失败**：固定两种 cause；原始错误保留，cleanup cause 有可诊断位置，stop 仍完成。不得悄悄把 cleanup 失败吞掉或替换启动 cause。
7. **失败回调反压与重入**：用 latch 暂停 nativeFailure（无需引 RimeMessageBus），确认另一个线程的被拒 dispatch 能结束，而 stop/restart 必须等待 callback 释放；覆盖从回调重入调用的策略。
8. **Lifecycle 等待与下一周期**：UNDISPATCHED 建立 whenReady waiter，再 fail，确认它结束；STOPPING 时新订阅是否允许等待下一轮要明确并测到；取消旧周期只做一次，不能在 ON_STOPPED 再误取消新订阅。
9. **快速失败→重启边界**：StateFlow 会合并状态。参考代码 fail 后紧接 ON_START，外部 scope 的 whenReady waiter 可能来不及观察短暂 FAILED；`job.cancelChildren` 只取消 registry 自己的孩子。要做确定性测试并决定第一项的代际终止契约，不能假定最终代码已证明旧等待者绝不会跑进新周期。这是源码时序风险，尚未实测失败。
10. **最小应用接线**：通过可控 engine/asset 故障，核对 Rime.nativeFailure 真的调用 registry.fail，既有 daemon create/restart/close 都有终态。纯 Dispatcher fake controller 测试不能替代这项接线验证。

参考 `RimeDispatcherTest.kt` 有 9 个案例，前 8 个大部分可复用；最后一个 failure-publication 案例当前依赖 RimeMessageBus、带 Commit/Deploy message，可用 latch callback 保留同一锁/屏障断言。参考 `RimeLifecycleTest.kt` 的 3 项可作为起点，但不覆盖上述所有交错。`SessionLaunchTest.kt` 的 7 项测试的是另一组 session owner 合约，第一项不应整包搬入。

未运行这些测试，也不承诺最终提取后数量与参考相同。实现时按真实新行为收敛测试，不按历史用例数验收。

## 后续两个独立问题

**异步启动与生命周期 UI 响应**：worker 化 JNI load/资产/主题初始化，daemon 外部锁内只变更意图；真正等待清理/重启应在生命周期 worker 完成。需要保持 startup/restart/last-session-close 互斥、通知终态与取消。自动存储重试可再单独加，不默认把 30 次 poll 或 Direct Boot exit 带入。

**消息输出不能丢 committed text/key**：替换 DROP_OLDEST 前，必须同时审阅消费者在 collect 中同步等待 native worker 的路径。背压会改变锁关系，不能只换 overflow 策略；独立验证提交/按键顺序、慢消费者、取消最后订阅者、嵌套 schema 通知与 failure publication。无需引 editor ownership 字段才能验证一般消息顺序。

## 如果第一项仍嫌大

可以先提交更小的 `fix: drain accepted Rime dispatcher work before stopping`：只做原子 start/stop 准入、独立 wakeup 与正常 stop 的 FIFO/屏障测试，再把失败与 lifecycle 接线作为下一项。但这个版本的标题、说明和验收必须限定正常停机；不得把它当作已经修复 startup failure 或所有 pending-call 悬挂。

不建议为减少文件数，把 FAILED 放进 dispatcher 却不接 Rime/lifecycle，或添加 IO fallback 却漏掉 runConfined 调用点。后两种都是不完整交付。

## 只读审阅的 Git blob 绑定

| 路径 | 基准 blob | 参考 blob |
| --- | --- | --- |
| `app/src/main/java/com/osfans/trime/core/RimeDispatcher.kt` | `b6a7991f13c31066879508b7791da5edbf52a297` | `59d14c04806ba4121abe0b7954c6d6022e75f6de` |
| `app/src/main/java/com/osfans/trime/core/RimeLifecycle.kt` | `929d067cd61e1ab701aa0aa0906e90b8e3ed29e4` | `7c23773a3cc00028623bca34f4a478f76fd475d1` |
| `app/src/main/java/com/osfans/trime/core/Rime.kt` | `e2e2d3d06a1551a751b48a67f4aa956c8701e3fa` | `79e44f2abb42d2c9977f4d5666701cf0ae8aa15c` |
| `app/src/main/java/com/osfans/trime/daemon/RimeDaemon.kt` | `b9245fa61f7db143a197ba0c0e32cd784704b3f7` | `44e5a207d27e94436754188cc78d1688d6245753` |
| `app/src/main/java/com/osfans/trime/daemon/Extensions.kt` | `a3c2b49d2a332eaf6e3bd530027f77368b889271` | `69cc7f9749637ff3700166854afa0fd38d2a87a9` |
| `app/src/main/java/com/osfans/trime/daemon/RimeSession.kt` | `000b5eeecef6924483980ae6e8396bae70e320ad` | `de894ca89d7bda3191c4543f9f298b9b2b249887` |
