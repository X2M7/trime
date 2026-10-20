# 上游贡献拆分清单

日期：2026-10-20。本分支仅用于 X2M7 fork 的拆分索引，不作为上游 PR 的内容。

上游基准是 `osfans/trime:develop` 的 `f5008260dc1e71b05e8e5508c6cfbf8ebf2507a0`；保留的原始贡献分支是 `contrib/complete-input-improvements`，快照为 `df12d0f4f62d7952a181e0ff7cf3cfd3e0077b7c`。两者之间有 67 个提交、660 个改动路径、90,665 行新增、3,388 行删除。最初一个提交就修改了 577 个文件，不能靠逐个 cherry-pick 达到“一件事一个 PR”。

维护者要求[拆成单一主题的小 PR 并清理补丁](https://github.com/osfans/trime/pull/2170#issuecomment-6279805216)。按照本次用户选择，先推送 fork 分支与清单，暂不创建新的上游 PR；[#2170](https://github.com/osfans/trime/pull/2170) 保持 Draft，原贡献分支与 fork 的 `develop` 不改写。

## 第一批独立分支

以下分支均直接建立在上述上游基准，各只有一个修复提交，相互没有依赖。不是把原有大分支换个名字，也没有带入 fork 发布、版本、签名、CI 或工具链策略。

| 分支与差异 | 单一问题 | 范围 | 已做检查 | 尚待完成 |
| --- | --- | --- | --- | --- |
| [split-setup-footer](https://github.com/X2M7/trime/compare/f5008260...contrib/split-setup-footer) | 向导内容占到导航按钮区域 | `activity_setup.xml`：ViewPager 约束到可见底部按钮顶部的 Barrier | XML 解析、按钮显示状态源码复核、AAPT2 资源编译、diff 检查 | Release/Lint；小屏、大字体及三页实际布局 |
| [split-clipboard-editor-height](https://github.com/X2M7/trime/compare/f5008260...contrib/split-clipboard-editor-height) | 长剪贴板内容的 wrap_content 高度不遵守已有下边界 | `activity_clip_edit.xml`：一个 constrainedHeight 属性 | XML 解析、单行差异复核、AAPT2 资源编译、diff 检查 | Release/Lint；长文本、小屏及软键盘实际布局 |
| [split-native-license-versions](https://github.com/X2M7/trime/compare/f5008260...contrib/split-native-license-versions) | 许可证信息中的 librime/OpenCC 版本落后于已固定源码 | 两个 JSON 的版本字段 | JSON 解析、其他字段不变、递归源码 pin 与实际版本核对、diff 检查 | 许可证页面集成检查；不涉及依赖升级 |
| [split-jni-local-refs](https://github.com/X2M7/trime/compare/f5008260...contrib/split-jni-local-refs) | 字符串数组转换积累 JNI 局部引用 | `objconv.h`：复用现有 JRef，每轮释放数组元素引用 | 真实生产头文件的聚焦宿主回归及修复前对照，详见验证记录 | Release/Lint、Android CheckJNI/设备验证 |

精确提交、改动文件与验证范围见 [branches.json](branches.json)。两个 XML 的 AAPT2 编译均退出 0，无输出诊断；这仅验证资源编译，不等于 APK 链接、布局测量或真机验收。宿主 mock 也不替代 Android 运行时。

JNI 聚焦测试在同一 mock 调用环境转换 2,048 个元素四次：原实现留下 8,192 个局部引用，修复后剩余 0、峰值 1，同时验证字符串顺序及先释放 UTF 字符再删除引用。真实 JNI 在 native 方法返回时会回收局部引用，本修复针对返回前的引用积累，不能据此声称发生跨调用的永久泄漏。宿主严格 `-Werror` 编译在两版都遇到相同四条已有 `pessimizing-move` / `sign-compare` 诊断；仅将这两类降为警告后运行对照，未顺带修改上游代码。因此这不是“完整构建零警告”的证明。[聚焦测试源码](checks/jni_local_refs.cc) 与复现命令见 [检查说明](checks/README.md)，保留在本 fork 索引，不把整套验收工具塞进两行修复。

首批四个分支的 GitHub CI 已通过：共 16/16 个 OS job 成功，每个分支在 Ubuntu、两种 macOS 和 Windows 实际执行 Debug 编译/打包及四 ABI 原生构建任务，JNI cache 均未命中；四个 Ubuntu Spotless 实际通过。[CI 审计摘要](checks/first-batch-ci.json) 绑定了精确提交与 run 链接。CI 未执行测试或 Lint，也没有下载审计 APK，因此不能写完整应用测试、Release 或设备验收通过。

原有共同诊断仍保留，包括 SHOW_IMPLICIT/srcDirs 弃用、原生依赖 CMake/constexpr/32 位窄化、SDK/JNA/Gradle，以及 Windows 缓存归档失败。四个分支按 OS 比较的诊断候选多重集合完全相同，相关源码/依赖 pin 与上游基准一致；这不是零警告。原大分支的历史验收不能替代这些新分支的验证。[贡献指南](../../CONTRIBUTING.md) 和 [PR 模板](../../.github/pull-request-template.md) 的相关验收项不能全部勾选；本次 Android Lint、Release 和手测仍未完成。本次不发布新的稳定版。

## 第二批：反馈资源与日志进程

同样直接基于 `f5008260`，两个分支互不依赖，均只有一个主题提交：

| 分支 | 实现范围 | 已执行验证 | 限制与待验收 |
| --- | --- | --- | --- |
| [split-lazy-feedback](https://github.com/X2M7/trime/compare/f5008260...contrib/split-lazy-feedback) | 保留 `init(context)`；实际启用时才创建 TTS/自定义 SoundPool；就绪后使用；销毁后的旧回调不能激活新资源；必要的 TTS package query | 实际生产 FeedbackResource 辅助类的 8 项 Kotest；主线程/回调身份与 Manager 调用路径独立源码审阅；Manifest XML 解析；Spotless/ktlint | helper 测试不能替代 Android Manager 集成和真实播放；初始化失败须 destroy/init 后再试；音效未加载前的按键不补播；未测内存收益 |
| [split-logcat-lifecycle](https://github.com/X2M7/trime/compare/f5008260...contrib/split-logcat-lifecycle) | Process/Logcat 终止与三流清理、取消阻塞读、错误保留、停止/重启及旧 Job 隔离 | 两个生产文件的 17 项 Kotest，包括真实 JVM 子进程取消、读取失败同时发生两次 destroy 失败、正常 EOF 的 destroy 失败；Spotless/ktlint | Android/Windows/macOS 实际运行待验证；原 logcat 命令选项不改，API21 的 PID/clear 兼容另拆；同步 destroy/close 不受退出轮询的 1 秒窗口限制 |

第二批合计 25 项测试均实际执行，失败/错误/跳过均为 0。使用上游固定的 Kotlin 2.3.21、Kotest 6.0.7、coroutines 1.10.2，JVM 目标 11，编译启用 `allWarningsAsErrors`，没有 Kotlin 编译警告。宿主工程由 Gradle 9.8.1 在 JDK17 上执行；保留 Gradle 弃用汇总；额外以 warning-mode all 强制执行格式检查时，观察到 Spotless 任务使用的 Configuration.setVisible(boolean) 将在 Gradle11 移除的诊断，原件保留。未因此混入工具链升级，也不宣称整个工具链无警告。其源码绑定、测试 XML 哈希和详细范围见 [第二批宿主验证记录](checks/second-batch-host.json)。该验证工程没有带入功能分支，也不是完整 Android 应用测试：日志宿主编译仅替换资源编号及本地化错误构造，反馈仅编译/执行纯资源所有者，Manager 另做源码/格式检查。

独立审阅发现并在推送前修复了两项边界：SoundPool 创建失败后每键重复文件访问；取消 watcher 销毁失败抢先覆盖原始读取异常。后者将 watcher 错误保存到单次调用的槽，等待结构化子任务完成后与最终清理错误合并，原读取/取消异常保留为主异常。

第二批的完整 Android CI、Lint、Release 与设备验收仍以各分支后续的精确 SHA 证据为准；没有用首批或原汇总分支的通过结果代替。

第二批 Commit CI 的日志分支已完成四系统 Debug/native 编译与 Ubuntu Spotless。首次 Ubuntu job 在执行任何 Gradle task 前无法解析两个 Kotlin 2.4.10 依赖，原件见[失败范围记录](checks/second-batch-ci-pending.json)；保持源码 `f5909dcb` 不变后的重试实际通过。重试界面的另外三个成功 job 沿用原运行，不算重新编译。反馈分支已有三系统通过，Intel macOS 仍在运行；[当前实际日志记录](checks/second-batch-ci.json)不把 pending 判为成功。全部已审成功 job 的诊断与首批同 OS 一致，既有警告保留。上述 Commit CI 均未运行测试或 Lint；完整应用 JVM 结果见下方单独控制验证。

## 第三批：回车动作与 JNI 线程归属

本批两个分支均已推送 fork，直接基于 `f5008260`、各只有一个提交，无相互依赖。JNI 线程分支四系统 Debug/native 构建与 Ubuntu Spotless 已通过；回车分支 Ubuntu、ARM macOS 与 Windows 已通过，Intel macOS 尚在运行。[实际日志审计](checks/third-batch-ci.json) 保留精确 SHA 与待定范围。Enter Windows 的既有缓存锁归档警告读取长度由 38 变为 37 字节，原文差异保留；仅对这一动态长度做规范化后与同 OS 基准比较，未发现新增源码诊断，不能声称原始警告逐字全同。

| 分支 | 单一问题与范围 | 已执行验证 | 限制与待验收 |
| --- | --- | --- | --- |
| [split-enter-actions](https://github.com/X2M7/trime/compare/f5008260...contrib/split-enter-actions) | 让回车显示与实际派发使用同一 `EditorEnterAction` 规则；只改 Enter delegate、service 的回车逻辑及纯辅助类/测试 | 实际 12 项 Kotest 全通过，0 失败/错误/跳过；四个最终源码 SHA 与记录核对；helper/test 编译，delegate/service 仅格式与源码复核 | 完整应用编译/单元测试已见下方控制验证；真实 EditorInfo/主题标签及 `performEditorAction` 行为待验收；Gradle 弃用诊断保留 |
| [split-jni-thread-attachment](https://github.com/X2M7/trime/compare/f5008260...contrib/split-jni-thread-attachment) | `JEnv` 只 detach 自己成功 attach 的线程，禁止所有权复制/移动；仅一个生产头文件 | 真实生产头文件/工厂配合 JNI 函数表 mock：7 个案例通过，原版 4 个案例失败；独占所有权静态断言前失败后通过；两版普通 C++17 严格编译无诊断 | 无 ART/JVM/Rime 回调执行；失败 attach 的 null 环境、detach 返回值与外部线程操作边界保留；四系统 Android Debug 构建已通过；CheckJNI/设备待验收 |

验证原件绑定见[第三批回车宿主记录](checks/third-batch-enter-host.json)与[第三批 JNI 记录](checks/third-batch-jni-thread.json)，最小复现源码和新复现命令见[检查说明](checks/README.md)。本批 12 项辅助类测试与此前 25 项宿主测试分开记录，不将格式检查等同于 service/delegate 集成测试。新 JNI shell 复现器也已实际执行通过：修复版 7 个案例通过，原版 4 个失败；严格编译无诊断，见[复现结果](checks/jni-thread-reproduction.json)。它与原执行属于同一组 7 个案例的两次运行，不累加成新的测试案例。

[补丁机械检查](checks/patch-composition.json) 已逐一在临时 index 上验证 8 份补丁相对基准可应用，并按记录顺序成功组合，得到 tree `2f8317b32873ed1bcb5694462494755bb5f9dc2c`；真实工作树与分支未改动。这个组合 tree 未编译或运行，只证明文本补丁没有应用冲突，不是集成验收通过。

## 独立的完整应用测试控制分支

[contrib/split-validation](https://github.com/X2M7/trime/tree/contrib/split-validation) 是 fork 专用控制分支，不计入 8 个源码模块，也不带进功能分支或上游 PR。第一轮工作流提交 `cc126ed9`，第二轮仅更换固定源码矩阵、正常前进到 `c519e3e9`；原运行与提交均保留。两轮真实执行 `:app:testDebugUnitTest --rerun-tasks`，原 XML/HTML、完整日志、20 个递归子模块身份和官方附件校验均已复核。

| 精确源码范围 | 实际应用 JVM 测试 | 运行 |
| --- | --- | --- |
| 上游基准 `f5008260` | 301 项 | [第一轮](https://github.com/X2M7/trime/actions/runs/39401035244) |
| 反馈 `3912fb23` | 309 项，新增 8 项 | 第一轮 |
| 日志 `f5909dcb` | 318 项，新增 17 项 | 第一轮 |
| 首批四个分支，以及 JNI 线程 `3e990b2d` | 每分支各 301 项 | [第二轮](https://github.com/X2M7/trime/actions/runs/39402432371) |
| 回车 `bcb47b1a` | 313 项，新增 12 项 | 第二轮 |

所有 scope 均 0 失败、错误和跳过；任务实际执行，没有把缓存结果记作重跑。按 `(classname, name)` 多重集比较，每支保留全部 301 项基准用例；Kotest 不同上下文中的同名短名称保留，不按唯一名称错误去重。重复执行的基准用例不累加为新测试案例。[第一轮摘要](checks/application-jvm-first.json)与[第二轮摘要](checks/application-jvm-second.json)绑定精确源码及原始记录哈希。

每个 scope 都保留同一条上游已有的 `ClipEditActivity.SHOW_IMPLICIT` Kotlin 弃用警告，Gradle/SDK/原生依赖等诊断也保留；不是零警告。完整应用编译意味着 Manager、service/delegate 已在这些 Android 工程中编译，但 JVM 测试仍不能证明真实 TTS、编辑器动作、布局、CheckJNI、Lint、Release 或设备行为通过。本次不发布 APK。

## 小 PR 草稿与提交顺序

[8 份英文草稿](drafts/README.md)逐项说明触发、行为、验证和剩余检查，均绑定各自源码 SHA，仅供审阅，未创建 PR。建议先送许可证元数据、向导底部约束这类边界明确的少数补丁；JNI 与交互改动补足对应设备复现后再逐项提交。不要一次向维护者提交 8 个尚缺手测的 PR。

## 关联 issue 的审慎匹配

[匹配记录](checks/issue-match.json) 保留六个原主题的只读检索结论与相近讨论，以及新主题的范围说明；未确认可直接关闭的精确开放 issue，不填写虚假的 `Fixes`/`Closes`。这是关键词和部分历史讨论的有限检索，不是全库不存在问题的证明；新的 JNI 线程主题也不冒用其他 JNI 崩溃报告。

## 后续拆分顺序

下面是重建路线，不表示这些功能已经提取、推送或通过。依赖指实际行为契约，不是必须提前提交的大型“公共基础设施”补丁。每项只从共享文件摘取所需代码与最小回归；若摘取时发现新的依赖，先更新清单，再决定合并同一功能或调整顺序。

### A. 优先选择独立问题

| 主题 | 边界 | 验证重点 |
| --- | --- | --- |
| 反馈资源后续设备验收 | 独立实现已见第二批；隐私钩子留给隐私补丁 | 实际 TTS/音效及 Android 生命周期；不重复提交第二批代码 |
| 旧 Android logcat 命令兼容 | 子进程生命周期已见第二批；另提 PID 过滤与 clear 选项 | API21 实际命令、完整 PID 匹配、不把日志正文当 PID |
| 图标字体启动开销 | Compact CommunityMaterial、固定字形表、来源/许可证与生成器 | 名称/字符/字体与原依赖一致；冷启动实际测量；独立回归，移除整套 T13 探针依赖 |
| 离线生成许可证 | AboutLibraries 离线数据与缺失条目 | 无 GitHub 请求、内容完整；不顺带升级插件 |
| 回车动作后续集成验收 | 独立实现已见第三批；只验证回车契约，不并入全部 EditorPolicy | custom actionId、NO_ENTER_ACTION、多行、搜索/发送及真实主题标签 |
| Shift 双击尊重主题动作 | KeyView/Keyboard 的 modifier 逻辑，属于上游已有双击 Shift 的回归修复 | 上游 `8545278b` 已有基础双击；自定义主题动作不被固定切换覆盖，不作为新增 CapsLock |
| 系统敏感剪贴板过滤 | 新建只读 SensitiveClipboardCompat；ClipboardHelper 在去重和异步入库之前拒绝 marked clip，两个生产文件与一个聚焦测试 | marked 文本不写入历史，也不生成新建议；普通内容及 API 分支保持；不清除既存普通建议/历史，不宣称完整 T07 |
| 格式检查跳过生成目录 | Spotless fileTree 一处 | 源码仍被检查，build/.cxx/.gradle 不被遍历；不改 ktlint 版本 |
| 原生构建并行度 | CMake Ninja job pool 独立代码块 | 未设置、有效及非法并行度；不带第三方覆盖层 |
| 构建时间戳单位 | timestamp 解析器、getter、聚焦测试 | 秒/毫秒及非法值；不改展示版本或发布策略 |
| ABI 参数校验 | ABI 解析器与 NativeBase 窄调用 | 默认值、合法覆盖、重复/空/非法值 |
| JNI 线程归属后续设备验收 | 独立实现已见第三批；保持既有错误返回约定 | 真实 native 回调/CheckJNI；不重复提交头文件或新增通知回调异常 |
| 预编辑浮层触摸与窗口 token | PreeditDelegate、删除额外触摸 PopupWindow，保留 InputView/service 的真实触摸区域 | API21 BadToken、浮层可点击、显隐及编辑器切换；不能只删除窗口而丢失触摸范围 |

这一组也不一次性创建全部 PR。先挑维护者认可且复现充分的 2–3 项，完成完整构建与针对性验收，再提交下一批。没有核实匹配的问题，不填虚假的 `Fixes #…`。

Shift 的主题动作回归与 `Keyboard.refreshModifier` 内 `result || setModifier` 短路造成的后续修饰键未更新是两项独立问题。原探针同时依赖后者，不能把整份探针或两个修复作为一项双击功能搬入；分别准备最小复现与补丁。

敏感剪贴板补丁先限制新内容进入现有唯一的入库/建议发布入口。若另外要求清除旧建议，应单独处理 nullable listener、InputBar 和旧的普通内容异步回调时序；不把这些行为默认算作“读取敏感标记”已经覆盖。

### B. 核心正确性与候选交互

| 主题 | 先决行为 | 单独验证 |
| --- | --- | --- |
| Rime 队列及失败处理 | [四文件最小闭环范围设计](scopes/rime-startup-failure.md)，尚未实现 | FAILED/ready 等待、原生幂等清理和 runConfined 必须一起接通；FIFO 只描述准入 Runnable，不等于跨挂起事务或消息输出无丢失 |
| 引擎异步启动与失败重试 | 队列契约 | 首次启动、部署失败、降级输入与重试 |
| 解锁后 Direct Boot 状态交接 | 队列/偏好生命周期 | 与上游已合入的 Direct Boot 修复去重 |
| 编辑器会话隔离 | 队列契约 | 切应用、服务重建、延迟响应及旧提交不会串框 |
| 输入框强制模式临时生效 | 编辑器边界 | 数字/邮箱/密码退出后恢复用户模式；与回车修复分离 |
| 第三方主题布局回退 | 主题加载和错误契约 | 缺失布局回退、自定义配置保留；不夹带大范围主题重写 |
| 候选刷新/点击交错安全 | 编辑器隔离 | 快照/revision、分页边界和候选身份；先不含预测、置顶、无障碍包装 |
| 展开/收起保留定位 | 候选身份 | 同一词与精确像素偏移，重新布局后保持 |
| 候选字号/注释/长句可读 | 稳定候选布局 | 大字体不裁切，长句可展开读全 |
| 可选候选横向滚动 | 稳定候选布局 | 滚动不误选，不与字号和置顶一起提交 |
| 忘记此词的反馈 | 候选身份 | 删除学习记录成功/失败反馈；不等同永久屏蔽 |
| 候选置顶/恢复排序 | 候选身份及持久化规则 | 固定词身份、重启保持、恢复默认；与删除学习记录分开 |

共享的 `Rime.kt`、`TrimeInputMethodService.kt`、`KeyboardWindow.kt`、`AppPrefs.kt` 等只能按功能块提取。不得把这些最终文件整份复制进某个“基础 PR”。

### C. 词库、隐私与新增输入功能

| 主题 | 边界及依赖 | 验证重点 |
| --- | --- | --- |
| 用户词库导入/恢复事务 | 字典文件/格式、维护排他与安全备份 | 失败和取消可恢复，覆盖安装后数据保留 |
| 常用短语编辑/导入导出 | 在安全词库维护之上增加编辑 | 格式、重复项、异常与重启持久化；不附带候选置顶 |
| 统一学习隐私 | 编辑器会话 + 原生学习/插件/上下文 + TTS/日志等全部内容出口 | 密码、NO_PERSONALIZED_LEARNING、手动隐私完整生效；正常学习恢复；不能仅隐藏 UI |
| 隐私会话剪贴板隔离 | 敏感标记过滤可先独立；手动/编辑器私密许可依赖完整隐私契约 | 延迟入库、会话切换、建议与用户明确复制动作 |
| 九键基础输入 | JNI/候选身份/主题回退；最小可用 schema 与布局 | 一次 JNI 快照、全拼/九键切换；先移除 predictor、模糊音与 onboarding |
| 九键拼音筛选/撤销编辑 | 九键基础 | 数字串、音节歧义、撤销及候选身份 |
| 九键模糊音与纠错 | 九键基础 | 固定虚构语料、开关与错误回退；先讨论策略 |
| 手动中英文状态记忆 | 临时强制模式；仅九键动作部分依赖九键 | 强制模式退出恢复、大小写与未提交拼音 |
| 数字/符号/邮箱布局 | 主题及动作规则 | 小数点、负号、@/./，减少切换但不丢拼音 |
| 下一词联想 | 候选身份 + 隐私 + 对应 schema | 小型离线模型、缺失回退、次数限制、按数字即新输入、不串框 |
| 输入延迟测量 | 队列及候选身份稳定后加少量可选打点 | 关联输入/显示/提交；显示可合并但按键不丢失 |

九键、联想等新增功能先按上游贡献指南讨论设计。不会通过给每个功能再附整套原生覆盖/设备探针来重复扩大补丁。

### D. 设置、存储与设备适配

| 主题 | 边界及依赖 | 验证重点 |
| --- | --- | --- |
| SAF 同步与恢复 | 路径约束、指纹/枚举、取消及 Worker；不夹带设置 UI | 授权、路径逃逸、失败恢复、用户文件不被覆盖 |
| 配置导入校验与原子激活 | 校验器、文件事务、取消提交边界、偏好/授权和引擎维护 | 失败保留旧配置，成功后才激活，不重复部署无关数据 |
| 首次设置选择布局/简繁并试打 | 已有 schema 选择及导入/主题契约 | 新装默认可输入，旧配置不覆盖，高级设置返回；底部 Barrier 已独立提取 |
| 常用设置入口 | 复用已有设置能力 | 状态读回、切换布局和方案一致，无关数据不部署 |
| 键盘按键无障碍 | 按键语义、hover、激活与动态回车 | TalkBack 独立探索/激活；TTS 朗读不能替代无障碍 |
| 候选/拼音筛选无障碍 | 候选身份；拼音筛选部分再依赖九键 | 选中状态、操作、旧节点激活不误选 |
| 横屏/分屏/大字体可见区 | 实际 viewport、insets 和触摸边界 | 候选/应用内容不遮挡；与语义实现分离 |
| 窗口导航栏和重建问题 | 先在上游已有具体 Activity 上重现并局部修复 | API21/新系统、对话框、旋转；暂缓整套新窗口基类 |

## 暂留 fork 或另议的内容

- 大型验收编排、数千行设备探针、截图 Observer、外部 TalkBack/eSpeak 补丁和性能语料保留在 fork；只将与具体问题有关的精简回归随功能提交。这不是以后再造一个“测试基础设施大 PR”。
- `PublicWindowPilotActivity` 及多组 Legacy/PublicPreferenceDialog 新抽象暂缓。原分支最后几个很小的导航栏提交依赖这些上游不存在的类，不能仅凭提交小就独立 cherry-pick。
- AGP/Kotlin/JDK 兼容策略、私有 Gradle API 适配、CI 重写、生成资源/缓存、图标和版本显示分别讨论，不作为普通应用修复的前置条件。
- OpenCC/librime 的词典 mmap、缓存恢复、Darts、解析与资源释放问题，先整理成依赖项目的最小补丁。再决定 Trime 采用固定上游版本还是必要的小适配；不直接搬入全部 CMake 文本替换覆盖层。本次不向其他仓库发布。
- fork 品牌、包名、签名、Pages、发布脚本、版本策略、历史构建验收流水不随应用功能上送。
- 真机、物理 ARM64/OEM、TalkBack 实际体验、长期驻留、16 KiB 页及 P95/PSS 等没有因本次拆分而获得验收结论。

## 查漏与操作规则

[paths.tsv](paths.tsv) 保留原比较的全部 660 个路径、增删行数及初步审阅归属。归属是粗分类，不是已验证的可执行补丁划分；尤其 122 个共享集成路径仍需逐块拆解。目前 14 个原路径有主题代码已提取，另外增加了 4 个重建后的聚焦辅助类/测试文件；未涉及的 646 个原路径仍仅作归类。触及某个路径不表示其中全部功能已提取，例如 Manifest 只取 TTS query、反馈管理器不取隐私钩子、Logcat 不取旧系统命令兼容。CI 控制分支不计入这个应用模块提取数量。

1. 每个新分支从当时核实的上游 `develop` 开始，独立问题无相互依赖；必须依赖的功能明确写出父补丁，等待父补丁合入后整理，不把累积差异冒充单个 PR。
2. 每个修复说明一个具体触发场景、前后行为及对应验证；同一共享文件内无关的 import、偏好、字符串和格式修改都留到所属补丁。
3. 送审前核对相关 issue/PR，补齐上游构建、全测试和真实复现；不把“源文件少”当作验证豁免。
4. 每次只向维护者提交少数完整小 PR，收到反馈后再推进下一组；本次仅推送 fork。
5. 原聚合分支、旧提交和发布产物继续保留，今后可通过这个索引追踪哪些行为已迁入独立分支。未取得新的明确指示，不关闭、重开或标 Ready 原 PR。

需要在本地继续某一分支时，可以递归克隆 fork，然后选择该分支：

```sh
git clone --recursive https://github.com/X2M7/trime.git
cd trime
git switch --track origin/contrib/split-setup-footer
git submodule update --init --recursive
```

实际版本以 branches.json 的 Git SHA 为准，避免之后分支移动导致误用另一轮验证结果。
