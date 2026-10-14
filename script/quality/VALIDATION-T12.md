# T12：首次使用和常用设置闭环

**双平台离线功能验收及本轮警告逐类复核完成；本记录冻结于发布前。** 当前产品源码 `d6199c86e7ad358158eec105d8ec46e1b27cdeff`，版本 `3.3.13-t9.15`／versionCode `20261120`。Build25、Android 15 `runtime11` 与 Android 5.0 `runtime27` 的完整功能验收均已闭合通过。Android 5.0／15 的 4,233／16,591 条原始运行警告保留；两系统均已逐类复核并在本轮已测功能范围内接受，系统内部原因和观测限制仍保留。双平台功能结论只对应本版本完整终态及 Root 独立审阅，不继承旧版本或局部通过。

## 已实现行为

- 首次使用串起系统启用与选用输入法、九键／全拼、简繁和实际中文试打；新安装默认应用专用存储及推荐全拼简体，无需手工编辑 YAML。
- 已有用户可保留当前方案与 custom 配置；明确应用选择后才调整 Rime 方案与简繁选项。高级设置使用独立标准 Activity，返回后保留未完成向导、输入选择与试打内容。
- 常用设置集中布局、候选字号、键高、触感、模糊音及词库入口；外观调整不触发无关部署。
- 配置导入分批读取、校验及暂存，成功后激活。失败恢复源文件、部署缓存、同步记录及存储选择；恢复失败保留备份路径。提交后的协程取消仍保持文件与目录设置一致。
- 试打页在可见页脚上方布局。回归使用真实系统触摸进入高级设置／方案列表并返回，检查完整可见按钮、通知权限取消及向导状态；滚动准备仅移动必要距离，记录静止 MOVE 与真实帧稳定，未用直接调用点击代替触摸。

## 当前构建与静态范围

| 检查 | Build25 的实际范围 |
| --- | --- |
| 应用 JVM | 新执行 775 项通过，78 份测试 XML；未把缓存计为新执行。 |
| 宿主检查 | `host15` 新执行 227 项通过。 |
| 其它 JVM | 17 项 build-logic＋6 项 compatibility 为 UP-TO-DATE，单列。 |
| 生产原生库 | `native-build-logs16` 的 Debug／Release × ARM64／x86_64 四组有本轮实际编译与链接，fresh4／reused0；不宣称所有第三方对象全量重编。 |
| 历史原生检查 | 原始 384 项及独立复测属于历史结果；当前 31 项输入绑定一致后复用，本轮未重跑，也不叠加覆盖。 |
| Lint／警告／签名 | Debug／Release 完整 Lint 均 0 问题；构建及双 ABI Release 签名检查警告 0。平台运行日志 W/E/F 另行逐类复核，不能以构建警告为零替代。 |
| 当前静态／接口 | `static15`／`release-linkage15`／callsite15／guard15 实际采集闭合；九个 DEX 方法共 750 code units、183 引用链及五处源码对应已独立核对。原采集器 `passed:false` 与设备／API 语义批准字段保持，不等于通用兼容性证明。 |
| 构建内存 | 单线程、JVM 堆 896 MiB／Metaspace 640 MiB，1 GiB 可用内存保护；本轮记录的最低可用内存约 5.5 GiB，builder 与守护正常退出。 |

本轮 Build25 摘要为 `build/t12/build25/summary.json`，SHA-256 `6f224b0f0b3117f0edc96420b067540d267bbd6b4237a4c6bbdde9b331e1e06b`。构建、31 输入绑定、实际 task 状态与接口事实的原始回执保留在本地证据中；不随三个手动 Release 附件上传。

## 当前设备验收范围

| 范围 | Android 5.0／API21 | Android 15／API35 |
| --- | --- | --- |
| 当前完整目录 | `build/t12/api21/runtime27` | `build/t12/api35/runtime11` |
| 本版本最终全 10 阶段 | 通过，当前源码／冻结 APK 的全 10 阶段闭合。 | 通过，当前源码／冻结 APK 的全 10 阶段闭合。 |
| Release 首次设置与常用设置 19 项 | setup-gui29 全 19 项通过，含五组汉字试打、实际 SAF 授予及取消。 | setup-gui11 全 19 项通过，含五组汉字试打、实际 SAF 授予及取消。 |
| 两主题真实点击／提交、旧版学习数据覆盖升级 | t11-gui25 两主题 6 项真实触摸检查通过；旧版学习数据覆盖升级及 Release 原生联想阶段通过。 | t11-gui11 两主题 6 项真实触摸检查通过；旧版学习数据覆盖升级及 Release 原生联想阶段通过。 |
| Debug 设置、导入失败／取消／回滚及既有回归 | 设置 14 项、严格导入回滚 7 项及既有回归通过；两主题候选索引 17 精确 −121px 收起／重开与两次重布局保持，40 轮显示及 100 次显隐通过。 | 设置 14 项、严格导入回滚 7 项及既有回归通过；两主题候选索引 17 精确 −121px 收起／重开与重布局保持，40 轮显示及 100 次显隐通过。 |
| 完整日志、all-PID fatal／LMK／kernel 与 Root 审阅 | 全 PID fatal／LMK／kernel critical 均 0；4,233 条 raw W/E/F 保留，逐类复核已在本轮功能范围内接受；系统与观测限制仍保留。 | 全 PID fatal／LMK／kernel critical 均 0；16,591 条 raw W/E/F 保留，警告 已逐类复核，在本轮功能范围内接受，保留性能与系统限制。 |
| owned VM／logcat 正常等待、真实限时交接 | 真实限时交接完成；VM／controller 实际退出均 0，清理错误为空，logcat 正常停止。 | 真实限时交接完成；VM／controller 实际退出均 0，清理错误为空，logcat 正常停止。 |

Android 15 本轮记录 420 个宿主内存样本，最低可用 3,427,068 KiB（约 3.27 GiB），1 GiB 保护未触发。Root 实际复核 14 张联想、52 张设置与 10 张编辑器原始 PNG 和配套记录；`runtime11-root-independent-review1.json` 只批准功能范围，`warning_gate_passed:false` 与 `stable_publication_ready:false` 保持。Debug 设置还核对了实际通知 Cancel 一次、最多两次 Back、七项几何数据及 Advanced 点击前四个稳定帧，原输入配置和非空草稿保留。Android 5.0 本轮记录 391 个内存样本，最低可用 4,257,588 KiB（约 4.06 GiB），VM／controller 实际退出均 0。Root 复核 54 张设置、14 张联想和 10 张编辑器原始 PNG。API21 无通知权限提示，因此通知 Cancel 为 0；两次 Back、80px 手势实际滚动 64px、四个稳定帧及原配置／非空草稿保持均通过。两系统 Observer9 均正常 stop，宿主／设备终态为 0。

每轮须绑定当前冻结 APK、源码、UID／boot／进程身份、原始命令与完整日志。真实交接必须在 45 分钟窗口内完成；Observer 保留 28 分钟／90 秒 idle／400 请求等原预算，正常 stop 与 host／remote 真正退出 0。功能结论来自完整终态、Root 实际图像审阅和全部业务断言；本轮警告已另行逐类复核，发布流程仍需复核最终门禁及资产。Android 15 guest 为 2304 MiB／4 核／host GPU；API21 保持 1536 MiB／1 核／swangle，宿主持续 1 GiB 门槛及实际 LMK／fatal／kernel 的验收规则不变。

## 保留的失败与修复范围

以下是此前验收中实际发现并保留的失败，不能当成本轮成功结果，也不提供新的警告豁免。

- 早期导入回滚改变编译缓存，以及提交后协程取消导致文件与目录设置不一致；相应回滚、取消边界已修复并加入应用 JVM 回归。Build15 Metaspace 警告受控停止，不用作发布验收。
- API21 早期一次性 uiautomator／Am 退出 SIGSEGV、候选滚动断言及旧证据采集失败均保留。Observer 改为有界持续会话；没有把系统进程 SIGSEGV 排除出全 PID 原始证据。
- API21 `runtime20` 的 ring log 丢失 START 标记；连续日志前缀方案保留原 ring dump、完整 START／END 身份与边界。`runtime21` 的 Release 19 项已完成但 Root 交接超时，整轮仍失败。
- API21 `runtime22` 的展开候选 partial-leading-row 偏移断言失败；修复测试对真实 scroll dy／稳定帧的观测。字形检查实际通过，未把该失败写成字体修复。
- Build20 的冗余 Kotlin cast 警告受控停止；Build22 后 Native13 发现旧 AGP JSON 与两个 x86 no-work，原失败保留。后续强制当前 metadata／JNI 实际生成和编译链接，未 touch 旧证据或削弱 fresh 条件。
- API21 `runtime24` 的候选 FrameLayout 子树在无障碍 XML 中缺失，原失败保留。当前 H9 在每次层级采集前通过公共 serviceInfo 刷新，保持同一会话及配置；原 XML／PNG／前后一致、候选 TextView 与 19 项业务断言未放宽。后续旧 H8 身份误配也保持失败，不算输入法已验收。
- API35 早期 helper 期限、Root 工具形状及限时交接失败保留。`runtime7` 试打页页脚重叠与高级设置返回路径经布局及独立标准 Activity 修复；真实 guest LMK 保留，不改称 Trime 崩溃或零风险。
- API35 `runtime8` 的通知窗口焦点等待、`runtime10` 的 Cancel 完整可见判定失败均保留。后者原来混合 root-global 与 screen 坐标；改为 local 可见矩形比较，真实指针仍使用 screen 坐标，并输出七项实际几何数据。失败时未发生 Cancel 触摸的记录不改写为“取消后失败”。
- 短诊断曾出现过量滑动后 Advanced 点击未触发，也有相同 APK 的 14 项局部通过。Build25 只改触摸准备：必要距离加 touch slop、至少 200ms 静止 MOVE、四个真实稳定帧后唯一点击。源码方案不是当前双平台完整验收，也不能直接证明私有 EdgeEffect 已停止。
- 旧 API21 FAT／孤立簇与环境诊断保留。离线修复只作用于新副本，原 reachable 文件／目录及回收数据核对、随后独立 AVD 物化作为环境证据；不改 SDK 原盘、不删除原警告，当前 runtime27 已核对 home2 实际只读／可写 FD、VFAT 挂载及四个 REC 文件字节；SDK 原盘未改，这些证据不代表全部客体存储健康。

## 限界与发布

Android 15 的警告采用逐类复核：Root 阅读了 420 个日志类别的代表行与计数、覆盖 105 条记录的 103 个控制台模板，以及项目相关全部子型和关键原行；没有宣称逐条亲读 8,945 个不同日志模板。原 16,591 条 W/E/F 与完整控制台未删改。Debug 有效配置导入实际等待 3305ms 后执行，属于保留的延迟限制，不能作为候选 P95≤100ms 的证据。系统 `install_sessions` 有四次 AtomicFile rename 失败，成功安装／升级不证明系统安装会话持久化健康。`audit_lost`／rate-limit 记录意味着不能保证底层审计事件无丢失；完整保留已采集原文不等于完整观测所有系统事件。未知的系统内部原因和平台风险未由本版修复。Android 5.0 的 4,233 条警告已逐类复核：114 个 tag／level 类别代表与计数、176 种控制台原文对应 301 条记录、13 个由 am_proc_start 明确绑定的 Trime PID 的 126 行／50 种消息，以及特定异常原行。保留三个其他应用资源泄漏栈、system_server 六次偏好目录创建失败、LatinIME 回调超时、启动阶段旧 APK 路径扫描与旋转冻结、memtrack／内存压力设施缺失和二进制事件日志溢出。未证明这些系统问题无害，也未由本版修复；观测到 LMK 为零不保证不会 OOM。

真机候选 P95≤100ms 或改善 30%、Java／native／graphics／总 PSS、连续输入与换主题后长期驻留、首次部署、物理 ARM64／全部 OEM／16 KiB 页、真实 TTS 和全部第三方方案／插件尚无本轮验收证据。模拟器响应断言不代替 P95；Debug 的 fullCheck 维护流程不代表正式版启动性能。候选展开 page48／prefetch16／maxSize144 是 best-effort 预算，不是硬驻留上限。

稳定门禁、完整失败历史清单与文档冻结：发布流程会复核稳定门禁、有限失败历史清单、冻结 APK 与最终文档；实际门禁回执和最终文档提交记录归档外保存。同步 fork 与稳定版发布：发布流程会复核三个附件并同步 fork develop 和版本 tag；本记录冻结于发布前，在线结果以版本 Release 页面为准。计划手动附件仅两个 Release APK 和含主仓库＋21 个递归子模块、共 22 个仓库的完整源码包，GitHub 自动 source zip／tar 保留。归档大小／SHA、公开下载复核、Latest 与旧附件清理在归档外记录，不把包含本文的源码归档 SHA 嵌回本文。

版本冻结与附件信息见 [v3.3.13-t9.15 验证记录](VALIDATION-t9.15.md)；此前稳定版的独立范围见 [t9.14 验证记录](VALIDATION-t9.14.md)，不能沿用为本版本批准。
