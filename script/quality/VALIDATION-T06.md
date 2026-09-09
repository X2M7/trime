# T06 输入框与生命周期验收

**`stable-build6` 的冻结APK已通过本报告列明的构建、宿主及30组运行验收；逐项日志裁决无未解释的发布阻断。**
范围为Android5.0/15模拟器及ARM64转译，历史负向对照和准备失败不计入通过组数。
这是冻结APK的限定范围验收；源码/发布附件绑定须独立通过，不能由本页代替实际发布验证。
本报告不把构建通过、功能断言通过或零项目诊断等同于零警告、无性能风险。

## 冻结身份

当前候选唯一来源为 `build/t06-runtime/stable-build6/identity.json`。
APK 构建提交：`dd87f879286a54e0d6e7a82afeee3abf6a82e0dc`。
`versionName=3.3.13-t9.8`，`versionCode=20261113`，包名 `com.osfans.trime.debug`。
最低 API21、target API37，保留 ARM64 与 x86_64；构建仍为 debuggable/debug 签名。
`BUILD_TIMESTAMP=1788973552491` 毫秒，即 `2026-09-09T17:05:52.491Z`。
构建环境、BuildConfig 的提交/仓库/时间戳相互一致。

| 冻结产物 | SHA-256 | 字节 |
| --- | --- | ---: |
| ARM64 应用 APK | `0587308c1452504d2f295778213883b7a4465027f4b220be62eaae07028f51aa` | 21261783 |
| x86_64 应用 APK | `c5599476e322e5d3d58de8d854156105580c15e7d2fd0f48327b6234d946d150` | 21357064 |
| Android 测试 APK | `6fca26d8b0009c25299f3a15df042e4e3c8aaed3cb825a28acefc23c2aff2ae2` | 861273 |
| 独立编辑器 fixture APK | `f39509702ef75911c7848908a8a17dd77be6b20a0aace78bca66094d38144233` | — |
| 独立 observer APK | `226a3add8818759a8f2135669f738abbd7d91091bad810b50dc9f2578181e0d8` | — |

发布应用文件名为 `trime-v3.3.13-t9.8-{arm64-v8a,x86_64}.apk`，重命名不得改变字节。
最终标签/发布文档提交及附件实际名称由独立 `release-identity.json` 记录。
后续仅改文档的提交不是新的 APK 构建提交；此文不提前自引用未知的最终 Git 提交。

## 已修复的行为

- 按输入框类型选择临时 ASCII/数字布局，返回聊天框恢复选定九键和模式。
- 回车文字与实际动作使用同一策略，覆盖发送、搜索、下一项、自定义动作和换行。
- 编辑器连接拥有独立标识，拒绝旧连接的提交、候选、排队按键、对话框和选项回调。
- 隐藏时取消长按/连发；物理键盘在受限输入框内遵循 Android 编辑器处理路径。
- 第三方主题缺少必要布局时以内置兼容布局回退，切换方案清理旧布局锁。
- 启动/部署不可用时保留数字降级输入、重试和系统输入法入口，并验证实际词典可用性。
- 修复 Android5 浮层提前显示的 BadToken；按锚点附着、可见性与布局状态管理浮层。
- 消息使用 64 帧有界缓冲和工作线程背压，故障通知移出调度锁以避免阻塞并发接单。
- 修复大字体候选注释裁切、剪贴板多行编辑区覆盖按钮、通知权限对话框窗口泄漏。
- 数字键盘用现有“空格”标签替代窄键位中的长方案名，保留 SPACE 动作代码。
- 构建时间统一为毫秒，避免旧秒数在“关于”/设备信息中显示为 1970 年。

## 构建与宿主检查

`stable-build6` 用时 607.438 秒，从干净源码完成；479 项源码哈希前后及复核时一致。
应用 JVM 285 项通过；build-logic 9 项成功 XML 保留，任务为 UP-TO-DATE。
两组均无失败、错误或跳过；应用测试任务实际执行，不能称 build-logic 全量重新执行。
Spotless 完成、Lint 0 项；本次完整构建日志没有 warning/deprecation 行。
`stable-host4` 的 102 项通过：quality58、T04 13、T05 5、baseline26。
30 个宿主源码哈希和四组日志哈希匹配；忽略目录摘要器测试不计入这 102 项。
两应用各 48 个资源与 checksums.json 匹配；ZIP 16KB 对齐和 ELF 三个 PT_LOAD 对齐/同余通过。
依据：`release-preflight-review.{json,md}`、`apk-verification.json`、JVM XML、Lint 和 host4 报告。

## 最终候选覆盖矩阵

下表只写 `stable-build6` 的证据；此前版本号相同的候选不能代替本轮 APK。
证据根目录为 `build/t06-runtime/`；公共归档按附件清单绑定这些明确选择的证据。

| 范围 | 本轮结果与边界 |
| --- | --- |
| API21 七项 instrumentation | PASS：editors、engine、startupFailure、feedback、shutdown、setup、clipSave |
| API21 editors | 浮层、输入/回车矩阵、旧编辑器隔离、第三方回退、真实 WebView、完整100次显示/隐藏通过 |
| 数字 SPACE | API21/API35 各两主题×PHONE/NUMBER共4项字形/动作代码检查通过；独立截图另核对窄键标签 |
| API21 独立应用 | PASS：17检查点，进程3949→4433恢复、旋转、切应用、各输入框与返回九键 |
| API21 不可用/重试 | PASS：18降级按钮无导航遮挡；实际 STABLE2→STABLE；Retry 后6→4出现 ni |
| API21 导航 | PASS：两轮 Profile/Back 和测试输入/Back；真实 ClipEdit/Trime 显示/实际 Cancel |
| API35 主探针 | PASS：editors、setup、engine、startupFailure、feedback、shutdown、clipSave、saf、clip、t02，共10项 |
| API35 editors | 输入/生命周期矩阵、硬键输入、真实 WebView、数字 SPACE 与完整100次显示/隐藏通过 |
| API35 通知生命周期 | PASS：5项；重复提示、旧 dismissal、重建、遮盖/延迟回调、finish 清理 |
| API35 横屏大字体 T03 | PASS：800×412dp、font2、两主题 normal/left/right/height80，共8张实图；geometry-only |
| API35 大字体 ClipVisibility | PASS：短/8行真实 Back、文本/原窗口保留、标题/按钮可见、实际 Cancel、完整设置恢复 |
| API35 完整 T03/T05 | PASS：两主题完整手势/按键语义和全拼真实你好提交；24条候选列表不变、24项显式音节恢复通过；8+2张原图复核 |
| API35 独立应用/分屏 | PASS：23检查点，杀进程恢复/旋转/切应用/两分屏位置，实际 STABLE→STABLE你→STABLE你你并保留；23张图复核 |
| API35 不可用/重试与导航 | PASS：实际2/delete、Retry后6/4恢复ni，6张图与资源原字节恢复；navigation-only两轮，不含ClipEdit |
| ARM64 真实旧版覆盖升级 | PASS：真实t9.6 ARM64→本轮ARM64，已安装SHA匹配，53字节独立标记完全保留 |
| ARM64 转译engine/T02/独立应用 | PASS：engine与T02真实JNI/输入，独立17检查点及17张原图复核；准备不计入正式检查点 |
| 汇总与发布绑定 | 最终选择30组：API21 10、API35 17、ARM 3；源码/附件必须通过下述独立校验门禁 |

API21 采用 Android5.0.2 x86_64，API35 采用 Android15 x86_64 模拟器。
独立应用的密码字段证据仅核对经删减的类型/光标状态，不声称密码内容或长度。
API35普通字段由Intent预填；分屏的6/4/SPACE实际提交中文，退出分屏后仍保留。
两位置Task113均为multi-window；6项键区像素检查支持非空键盘，不是OCR。
顶部编辑器分屏截图中IME占用了另一应用的区域，不声称两应用全文同时可见。
降级测试精确恢复default.yaml和checksums.json原字节，不称全资源树校验或实际汉字提交。
API35外部共29张原始PNG已由专项review逐张查看；navigation-only没有PNG或ClipEdit操作。
数字 SPACE 专项验证动作代码与真实字形边界，不能称它已实际点击空格并验证文本提交。
API21/API35 editors 的 hide/show 保留同一连接组合；旋转/连接重启可能取消未提交组合。

## 性能与故障注入的明确范围

| 探针最大主线程循环间隔 | API21 | API35 |
| --- | ---: | ---: |
| engine | 316 ms | 769 ms |
| shutdown | 68 ms | 72 ms |
| setup | 230 ms | 1344 ms |

以上是这次运行观察值，不是持续性能上限；setup 门限为原有 4000 ms。
ARM engine初始化/维护到ready为100.528秒，2308次心跳、最大间隔766ms，门限仍为4000ms。
其Application idle等待142ms；T02的idle等待406ms是另一次同步测量，均不称按键/屏幕延迟。
ARM所测区间未出现2000ms阈值的队列等待诊断，不证明所有环境的偶发等待已根除。
两平台 setup 均处理300次刷新、29次导航点击；API21不支持通知权限流程，明确跳过该5项。
engine 的13条项目诊断来自有意缺失 `__missing_anr_test__` 主题及传播堆栈。
startupFailure 的72条来自资源复制阻塞、外部同步不可用、缺失方案、JNI长名和坏YAML。
这些测试检查有界等待、降级、重试、可用词典与缓存/资源恢复，注入标签不豁免崩溃。
ClipSave 每平台8项（剪贴板4、收藏4）使用 LatinIME；不能算作活动 Trime 输入覆盖。
两次故意删除待保存行验证文本/缓存保留：API21有66条失败堆栈，API35有28条。
API21 ClipSave 另有11条 inactive cursor-anchor 项目诊断，所有54条 inactive IC 警告保留。
API35 SAF 为21项隔离 provider 检查；3次故意 rename 失败产生78条框架警告/堆栈。
该 SAF 探针不等于外部系统/云 provider 持久授权或所有 OEM 兼容性验收。
feedback 包含200次描述符扫描、真实 SoundPool 加载/播放/释放；系统音频警告仍保留。

## 图像、窗口与恢复证据

T03 大字体8张原始 PNG 均已独立查看并核对 SHA；候选汉字/拼音注释完整。
每个布局17个目标至少48dp，候选/组合/侧栏/九键不重叠，左右单手及height80均通过。
完整360dp T03另有两主题8张图，完整T05有2张图，均已由专项review逐张复核。
因此本轮T03共16张（完整8＋大字体8），T05为2张，不混计历史截图。
T05的24个off/on原始候选列表一致；模糊恢复2/12→12/12，纠错恢复1/12→12/12。
这24项恢复需显式选择音节，不是自动Top-1；21个新增恢复有对应来源位，3个原有恢复保留sources=0。
完整指标见`api35-final-t03-t05-review.json`：首键/末键加候选耗时不含屏幕呈现，采样PSS不是峰值。
这是一次顺序debug x86_64样本，不能据此推断因果速度/内存改善；规则默认关闭。

ClipVisibility 的11次 observer 层级均新鲜，61个文件与报告 SHA 清单匹配。
6张关键图已查看：短/8行各 Trime显示、Back后、Cancel后。
短文本字段 `[532,207][1068,543]`，按钮起点y543；8行字段 `[532,207][1068,578]`，按钮起点y578。
两者字段/按钮重叠面积为0，标题/OK/CANCEL字形完整；实际Cancel后Activity关闭。
Back后保持原window token及全部文本。8行自然滚至光标末尾，实图显示部分Line4至Line8。
未手动滚回首行、未点OK或写数据库；保存功能由独立ClipSave探针覆盖。
键盘显示时adjustPan可能移出标题/按钮；实际Back恢复同窗口中的完整可用控件。
Clip finally 对尺寸、密度、font1.0、旋转0和默认Trime输入法逐字段读回，完全恢复。
T03原始显示状态与随后Clip preflight一致；T03自身没有单独最终读回文件。
API35 主 `clip` 探针只在设备缓存生成并检查非空PNG，本次未拉取该PNG，未独立看图。
它不能与另外有保留图像的 ClipVisibility 混为一项图像验收。

## 运行日志与单独裁决

API21七探针全应用范围保留404条W/E；editors全范围73条含准备阶段1条inactive诊断。
该诊断在01:18:23.566、LatinIME准备阶段，早于01:18:24.585激活标记；活动范围64条W/E、0项目诊断。
API21独立应用/不可用/导航分别保留5/72/6条W/E；不可用的67条项目错误均保留注入来源。
API35对应范围为40/76/46条W/E；不可用的67条项目错误均源于故意资源阻塞。
API35外部保留IPC事务失败、导航动画FrameTracker及实际kill/替换进程记录；继续通过不证明性能风险消失。
具体PID/窗口/系统原因、全部警告及SHA见`api35-final-external-review.{json,md}`。
API35十主探针保留638条W/E；完整T03/T05各22/17条，大字体T03另18条、ClipVisibility另24条。
这些警告和错误均保留，不能称零警告。
这些计数对应不同明确范围，不把同一进程的全量/活动子集或重复全系统backlog相加为新事件。

API35 editors主PID2039在01:24:59.170报告renderer2154 `crash detected (code -1)`。
01:24:59.188系统以`isolated not needed`终止2154；01:24:59.294 Zygote记录`exited cleanly (0)`。
冻结探针在WebView真实邮箱输入断言之后销毁WebView，再继续完成100次循环。
据此推断与该次WebView收尾关联；原始critical仍保留，不能写作未发生任何renderer诊断。
后续全系统捕获含同一时间/PID记录，是同一事件backlog；其他PID/时间仍须分别裁决。
这是1个独立事件，在后续完整日志中重复保留；自动摘要保留每次命中，不能将backlog当作新事件。
最终`stable6-final-summary.json`的30组证据检查错误为0；原始critical仍保留，自动退出码仍为2。
其SHA为`2478410d8d224fe14ed72ee7c3a8c10cd1a17d4823d43695aff6a78e59617d2c`。
`stable6-runtime-adjudication.{json,md}`逐一核对44个critical ID，48个文件出现位置、10条不同原始行、8个事件。
事件为上述renderer及7个更早的ARM准备期SystemUI/Google ANR（另有2条关联窗口行），不是44次新崩溃。
全部614条启发式项目诊断出现位置均复核；额外01:24:04.144布局警告对应SystemUI PID794，并非之后的Trime PID8187。
统一裁决`acceptance_passed=true`，未解释/未匹配/未审查项为空；仍保留`warning_free=false`。
自动警告总数含不可用测试的全系统backlog，不是独立故障总数；各应用区间按上文分开列示。
API21七探针完整critical扫描为0；外部日志保留此前BootReceiver导入及故意kill后的服务重启措辞。
这些旧措辞不改写为新的应用崩溃，也不证明未检查的启动期tombstone普遍无害。
FrameTracker超时/丢帧、图形/音频、回调取消及所有故障注入堆栈均保留原文。
ARM engine/T02/独立应用分别保留27/24/43条W/E；engine13条项目诊断均为故意缺失主题。
ARM初始化较慢、IPC/动画/图形警告保留；本轮已审查活动区间无未解释FATAL/ANR/窗口泄漏或队列等待。

## 签名与兼容范围

证书SHA-256：`c6fb875b1959f56fc6b3992d603b86e2dcd793677d32aba83687b34ef6937dd2`。
两应用v1/v2均验证通过，但每份保留3项META-INF签名覆盖警告；测试APK为0项。

- `META-INF/org/jetbrains/kotlinx/kotlinx-serialization-core-jvm/verification.properties`
- `META-INF/services/kotlinx.coroutines.CoroutineExceptionHandler`
- `META-INF/services/kotlinx.coroutines.internal.MainDispatcherFactory`

协程ServiceLoader文件保留，未为减少警告而删除。Android5/6的v1支持继续保留。
GitHub“稳定版”状态不改变debug签名、debuggable包或Gradle变体；未换生产密钥、未启用release/R8。
没有物理ARM/OEM编辑器或物理16KB页设备证据；静态ZIP/ELF对齐不等于16KB设备运行。
ARM35实际为2GB/2核Google API35 x86_64模拟器，经Berberis(aarch64)0.2.3/native bridge加载ARM64 JNI。
独立17项为全屏状态/旋转/切应用/进程恢复，不含分屏或新输入中文；实际中文单次提交由T02覆盖。
横屏辅助mode文字有部分被键盘区域覆盖，实际输入框与全部键盘行完整，原图/限制均保留。
有界消息测试覆盖暂停订阅者、无订阅、取消解除背压及嵌套消息，不保证任意插件/背压永远无损。
可用性恢复验证标准词典，不能推广为非标准plugin-only translator已经恢复。

## 升级、附件与历史

API21/API35安装identity记录stable-build5 x86_64 `e7678a21…`覆盖为本轮`c5599476…`并保留独立标记。
这不是从t9.6直接升级的证据；独立标记保留也不证明所有个人学习词库/剪贴板已迁移。
真实旧t9.6 ARM64 `92e0e56244559b091801e8748f25a7fb2d80313938147c0ca006290e1d7ad9f6`已无卸载覆盖为本轮`0587308c…`。
`arm35-stable6-install/identity.json`确认已安装身份及53字节独立标记前后完全相同。
旧版中文输入就绪未单独测试；不能据准备记录推断已就绪或初始部署一定未完成。
旧版正常向导分阶段完成；系统ANR及UI假设/过渡导致的失败原样保留，见`arm35-preparation-review`。
本轮T9准备第一次未及时取得预期键盘XML，仍为FAIL；随后真实菜单选T9成功，不改写首次结果。
空失败XML与稍后已出现全拼的PNG不同步；12.718秒启动至键盘附着间隔不称输入延迟。
该helper未单独重测installed APK SHA，按最近安装及包记录限定关联，详见`arm35-t9-preparation-review`。
发布绑定附件为完整源码包、validation ZIP、`SHA256SUMS`、`source-verification.json`和`release-identity.json`。
源码包 `trime-v3.3.13-t9.8-source-with-submodules.tar.gz`须逐项匹配Git blob及递归子模块；GitHub自动Source ZIP不含子模块。
validation ZIP须通过逐文件SHA/CRC，发布文件、标签与公开下载须按清单再次验证；本页不能代替实际校验。
上述源码/附件绑定未闭合前禁止发布；APK测试通过不自动表示GitHub已完成发布或替换Latest。
发布归档只选择明确复核的证据；整机原始日志本地保留，公开review包含必要裁决片段。

[历史记录](VALIDATION-T06-HISTORY.md)保留原正文60755字节及原SHA `af43547a93fe18287c0ba3a1e1e0c2fefacd0a09cde61017d758969d1d38829e`。
历史中的Latest/Pending Acceptance/未提交等措辞属于原检查点，不再是本页当前状态。
`T06-dev-build1…11`指早期本地开发轮次，`stable-build1…6`指本轮稳定候选；同号不混用。
`stable-hostN`是独立宿主执行批次；目录名不能代替APK SHA。
stable-build2有测试装置未附着错误；stable-build3有候选/按钮裁切；stable-build4因WindowLeaked中止、无冻结APK。
stable-build5虽相关回归通过，仍因数字空格标签裁切被拒绝；旧应用配新测试的失败对照原样保留。

复现使用固定APK/test/fixture身份、一个专用模拟器及device_audit_lock；构建时停止模拟器。
API21 editors使用`--bind-ime-after-editor-focus`，避免instrumentation替换进程的IME绑定干扰。
独立进程恢复测试不使用该绑定辅助；故障注入只操作专用资源并在finally恢复。
归档选择须区分30组正式运行、历史负向对照、旧版向导失败和T9准备失败；失败记录不改写为PASS。
